#!/usr/bin/env python3
import struct, sys, glob, os

WIDTH = [1] * 256
for a, b, w in [(0x02,0x02,2),(0x03,0x03,3),(0x05,0x05,2),(0x06,0x06,3),(0x08,0x08,2),
                (0x09,0x09,3),(0x13,0x13,2),(0x14,0x14,3),(0x15,0x16,2),(0x17,0x17,3),
                (0x18,0x18,5),(0x19,0x19,2),(0x1a,0x1a,2),(0x1b,0x1b,3),(0x1c,0x1c,2),
                (0x1f,0x20,2),(0x22,0x23,2),(0x24,0x26,3),(0x29,0x29,2),(0x2a,0x2c,3),
                (0x2d,0x31,2),(0x32,0x3d,2),(0x44,0x6d,2),(0x6e,0x72,3),(0x74,0x78,3),
                (0x90,0xaf,2),(0xd0,0xe2,2),(0xfa,0xfb,4),(0xfc,0xfd,3),(0xfe,0xff,2)]:
    for op in range(a, b + 1):
        WIDTH[op] = w


class Dex:
    def __init__(self, path):
        self.path = path
        self.d = open(path, 'rb').read()
        d = self.d
        self.string_size, self.string_off = struct.unpack_from('<II', d, 0x38)
        self.type_size, self.type_off = struct.unpack_from('<II', d, 0x40)
        self.proto_size, self.proto_off = struct.unpack_from('<II', d, 0x48)
        self.method_size, self.method_off = struct.unpack_from('<II', d, 0x58)
        self.class_size, self.class_off = struct.unpack_from('<II', d, 0x60)

    def uleb(self, off):
        result = 0; shift = 0
        while True:
            b = self.d[off]; off += 1
            result |= (b & 0x7f) << shift
            if not (b & 0x80): return result, off
            shift += 7

    def string(self, idx):
        off = struct.unpack_from('<I', self.d, self.string_off + idx * 4)[0]
        _, p = self.uleb(off)
        end = self.d.index(b'\x00', p)
        return self.d[p:end].decode('utf-8', 'replace')

    def type_desc(self, idx):
        return self.string(struct.unpack_from('<I', self.d, self.type_off + idx * 4)[0])

    def method(self, idx):
        c, p, n = struct.unpack_from('<HHI', self.d, self.method_off + idx * 8)
        return self.type_desc(c), p, self.string(n)

    def proto_sig(self, proto_idx):
        ret_idx, params_off = struct.unpack_from('<II', self.d, self.proto_off + proto_idx * 12 + 4)
        params = []
        if params_off:
            size = struct.unpack_from('<I', self.d, params_off)[0]
            for i in range(size):
                t = struct.unpack_from('<H', self.d, params_off + 4 + i * 2)[0]
                params.append(self.type_desc(t))
        return params, self.type_desc(ret_idx)

    def find_string(self, literal):
        lo, hi = 0, self.string_size - 1
        while lo <= hi:
            mid = (lo + hi) // 2
            v = self.string(mid)
            if v == literal: return mid
            if v < literal: lo = mid + 1
            else: hi = mid - 1
        return -1

    def find_type(self, string_idx):
        lo, hi = 0, self.type_size - 1
        while lo <= hi:
            mid = (lo + hi) // 2
            v = struct.unpack_from('<I', self.d, self.type_off + mid * 4)[0]
            if v == string_idx: return mid
            if v < string_idx: lo = mid + 1
            else: hi = mid - 1
        return -1

    def classes(self):
        for c in range(self.class_size):
            base = self.class_off + c * 32
            type_idx, _, = struct.unpack_from('<I', self.d, base)[0], 0
            yield self.type_desc(type_idx), struct.unpack_from('<I', self.d, base + 24)[0]

    def class_methods(self, data_off):
        if not data_off: return
        off = data_off
        sf, off = self.uleb(off)
        inf, off = self.uleb(off)
        dm, off = self.uleb(off)
        vm, off = self.uleb(off)
        for _ in range(sf + inf):
            _, off = self.uleb(off); _, off = self.uleb(off)
        for count in (dm, vm):
            midx = 0
            for _ in range(count):
                d, off = self.uleb(off)
                midx += d
                _, off = self.uleb(off)
                code, off = self.uleb(off)
                yield midx, code

    def walk(self, code_off):
        units = struct.unpack_from('<I', self.d, code_off + 12)[0]
        start = code_off + 16
        pos = 0
        while pos < units:
            unit = struct.unpack_from('<H', self.d, start + pos * 2)[0]
            op = unit & 0xff
            if op == 0x00 and unit != 0:
                if unit == 0x0100:
                    pos += struct.unpack_from('<H', self.d, start + pos * 2 + 2)[0] * 2 + 4
                elif unit == 0x0200:
                    pos += struct.unpack_from('<H', self.d, start + pos * 2 + 2)[0] * 4 + 2
                elif unit == 0x0300:
                    w = struct.unpack_from('<H', self.d, start + pos * 2 + 2)[0]
                    s = struct.unpack_from('<I', self.d, start + pos * 2 + 4)[0]
                    pos += (s * w + 1) // 2 + 4
                else:
                    pos += 1
                continue
            yield op, pos, start
            pos += WIDTH[op]


def desk(desc):
    return desc[1:-1].replace('/', '.')


def q_users(dx, literal):
    si = dx.find_string(literal)
    if si < 0: return []
    out = []
    for desc, cdo in dx.classes():
        for midx, code in dx.class_methods(cdo):
            if not code: continue
            for op, pos, start in dx.walk(code):
                if op == 0x1a and struct.unpack_from('<H', dx.d, start + pos*2 + 2)[0] == si: hit = True
                elif op == 0x1b and struct.unpack_from('<I', dx.d, start + pos*2 + 2)[0] == si: hit = True
                else: continue
                _, _, mname = dx.method(midx)
                out.append((desk(desc), mname)); break
    return out


def q_invoked(dx, class_desc, method_name):
    si = dx.find_string(class_desc)
    if si < 0: return []
    t = dx.find_type(si)
    if t < 0: return []
    out = []
    for desc, cdo in dx.classes():
        if cdo == 0: continue
        dsi = dx.find_string(desc)
        if dsi < 0 or dx.find_type(dsi) != t: continue
        for midx, code in dx.class_methods(cdo):
            _, _, mname = dx.method(midx)
            if mname != method_name or not code: continue
            for op, pos, start in dx.walk(code):
                if (0x6e <= op <= 0x72) or (0x74 <= op <= 0x78):
                    tgt = struct.unpack_from('<H', dx.d, start + pos*2 + 2)[0]
                    o, _, n = dx.method(tgt)
                    out.append(f"{desk(o)}#{n}")
    return out


def q_refs(dx, class_desc):
    si = dx.find_string(class_desc)
    if si < 0: return []
    t = dx.find_type(si)
    if t < 0: return []
    out = []
    for desc, cdo in dx.classes():
        for midx, code in dx.class_methods(cdo):
            if not code: continue
            for op, pos, start in dx.walk(code):
                if op in (0x1c, 0x1f, 0x22):
                    v = struct.unpack_from('<H', dx.d, start + pos*2 + 2)[0]
                elif op == 0x20:
                    v = struct.unpack_from('<H', dx.d, start + pos*2 + 2)[0]
                else:
                    continue
                if v == t:
                    _, _, mname = dx.method(midx)
                    out.append((desk(desc), mname)); break
    return out


def q_methods(dx, class_desc):
    si = dx.find_string(class_desc)
    if si < 0: return []
    t = dx.find_type(si)
    if t < 0: return []
    out = []
    for desc, cdo in dx.classes():
        if cdo == 0: continue
        dsi = dx.find_string(desc)
        if dsi < 0 or dx.find_type(dsi) != t: continue
        for midx, code in dx.class_methods(cdo):
            c, p, n = dx.method(midx)
            params, ret = dx.proto_sig(p)
            out.append((n, params, ret, bool(code)))
    return out


def iter_dex(root):
    for f in sorted(glob.glob(os.path.join(root, "classes*.dex")),
                    key=lambda p: (len(os.path.basename(p)), os.path.basename(p))):
        yield Dex(f)


if __name__ == "__main__":
    root = os.environ.get("DEXDIR", ".")
    cmd = sys.argv[1]
    if cmd == "users":
        for dx in iter_dex(root):
            for c, m in q_users(dx, sys.argv[2]):
                print(f"{os.path.basename(dx.path)}  {c}#{m}")
    elif cmd == "invoked":
        desc = "L" + sys.argv[2].replace('.', '/') + ";"
        for dx in iter_dex(root):
            for x in sorted(set(q_invoked(dx, desc, sys.argv[3]))):
                print(f"{os.path.basename(dx.path)}  {x}")
    elif cmd == "refs":
        desc = "L" + sys.argv[2].replace('.', '/') + ";"
        for dx in iter_dex(root):
            for c, m in sorted(set(q_refs(dx, desc))):
                print(f"{os.path.basename(dx.path)}  {c}#{m}")
    elif cmd == "methods":
        desc = "L" + sys.argv[2].replace('.', '/') + ";"
        for dx in iter_dex(root):
            for n, params, ret, has_code in q_methods(dx, desc):
                print(f"{os.path.basename(dx.path)}  {n}({', '.join(params)}) -> {ret}  code={has_code}")
    elif cmd == "find":
        needle = sys.argv[2]
        for dx in iter_dex(root):
            for desc, cdo in dx.classes():
                if needle in desc:
                    print(f"{os.path.basename(dx.path)}  {desk(desc)}")
