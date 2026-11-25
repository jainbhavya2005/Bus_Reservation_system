package util;

import java.nio.charset.StandardCharsets;

/** Minimal QR Code generator (byte mode, error correction level M, versions 1-10). No external libraries. */
public class QrCode {

    private static final int[] ECC_PER_BLOCK = {-1, 10, 16, 26, 18, 24, 16, 18, 22, 22, 26};
    private static final int[] NUM_BLOCKS    = {-1,  1,  1,  1,  2,  2,  4,  4,  4,  5,  5};

    public final int size;
    private final boolean[][] modules;
    private final boolean[][] isFunction;

    public boolean get(int x, int y){
        return x >= 0 && x < size && y >= 0 && y < size && modules[y][x];
    }

    public static QrCode encode(String text){
        byte[] data = text.getBytes(StandardCharsets.UTF_8);

        int version = -1;
        for(int v = 1; v <= 10; v++){
            int countBits = v <= 9 ? 8 : 16;
            if(4 + countBits + data.length * 8 <= dataCodewords(v) * 8){ version = v; break; }
        }
        if(version < 0) throw new IllegalArgumentException("Text too long for QR code");

        // ---- build bit stream ----
        StringBuilder bits = new StringBuilder();
        appendBits(bits, 0x4, 4);
        appendBits(bits, data.length, version <= 9 ? 8 : 16);
        for(byte b : data) appendBits(bits, b & 0xFF, 8);

        int capacityBits = dataCodewords(version) * 8;
        appendBits(bits, 0, Math.min(4, capacityBits - bits.length()));
        appendBits(bits, 0, (8 - bits.length() % 8) % 8);
        for(int pad = 0xEC; bits.length() < capacityBits; pad ^= 0xEC ^ 0x11) appendBits(bits, pad, 8);

        byte[] dataBytes = new byte[bits.length() / 8];
        for(int i = 0; i < bits.length(); i++){
            if(bits.charAt(i) == '1') dataBytes[i >>> 3] |= 1 << (7 - (i & 7));
        }

        return new QrCode(version, addEccAndInterleave(dataBytes, version));
    }

    // ================= construction =================

    private QrCode(int version, byte[] allCodewords){
        size = version * 4 + 17;
        modules = new boolean[size][size];
        isFunction = new boolean[size][size];

        drawFunctionPatterns(version);
        drawCodewords(allCodewords);

        int bestMask = 0;
        int minPenalty = Integer.MAX_VALUE;
        for(int m = 0; m < 8; m++){
            applyMask(m);
            drawFormatBits(m);
            int p = penalty();
            if(p < minPenalty){ minPenalty = p; bestMask = m; }
            applyMask(m); // undo
        }
        applyMask(bestMask);
        drawFormatBits(bestMask);
    }

    private static void appendBits(StringBuilder sb, int val, int len){
        for(int i = len - 1; i >= 0; i--) sb.append(((val >>> i) & 1) != 0 ? '1' : '0');
    }

    private static int rawModules(int ver){
        int result = (16 * ver + 128) * ver + 64;
        if(ver >= 2){
            int numAlign = ver / 7 + 2;
            result -= (25 * numAlign - 10) * numAlign - 55;
            if(ver >= 7) result -= 36;
        }
        return result;
    }

    private static int dataCodewords(int ver){
        return rawModules(ver) / 8 - ECC_PER_BLOCK[ver] * NUM_BLOCKS[ver];
    }

    // ================= Reed-Solomon =================

    private static byte[] addEccAndInterleave(byte[] data, int ver){
        int numBlocks = NUM_BLOCKS[ver];
        int blockEccLen = ECC_PER_BLOCK[ver];
        int rawCodewords = rawModules(ver) / 8;
        int numShortBlocks = numBlocks - rawCodewords % numBlocks;
        int shortBlockLen = rawCodewords / numBlocks;

        byte[][] blocks = new byte[numBlocks][];
        byte[] rsDiv = rsDivisor(blockEccLen);

        for(int i = 0, k = 0; i < numBlocks; i++){
            int datLen = shortBlockLen - blockEccLen + (i < numShortBlocks ? 0 : 1);
            byte[] dat = java.util.Arrays.copyOfRange(data, k, k + datLen);
            k += datLen;
            byte[] block = java.util.Arrays.copyOf(dat, shortBlockLen + 1);
            byte[] ecc = rsRemainder(dat, rsDiv);
            System.arraycopy(ecc, 0, block, block.length - blockEccLen, ecc.length);
            blocks[i] = block;
        }

        byte[] result = new byte[rawCodewords];
        for(int i = 0, k = 0; i < blocks[0].length; i++){
            for(int j = 0; j < blocks.length; j++){
                if(i != shortBlockLen - blockEccLen || j >= numShortBlocks){
                    result[k++] = blocks[j][i];
                }
            }
        }
        return result;
    }

    private static byte[] rsDivisor(int degree){
        byte[] result = new byte[degree];
        result[degree - 1] = 1;
        int root = 1;
        for(int i = 0; i < degree; i++){
            for(int j = 0; j < result.length; j++){
                result[j] = (byte) gfMul(result[j] & 0xFF, root);
                if(j + 1 < result.length) result[j] ^= result[j + 1];
            }
            root = gfMul(root, 0x02);
        }
        return result;
    }

    private static byte[] rsRemainder(byte[] data, byte[] divisor){
        byte[] result = new byte[divisor.length];
        for(byte b : data){
            int factor = (b ^ result[0]) & 0xFF;
            System.arraycopy(result, 1, result, 0, result.length - 1);
            result[result.length - 1] = 0;
            for(int i = 0; i < result.length; i++){
                result[i] ^= gfMul(divisor[i] & 0xFF, factor);
            }
        }
        return result;
    }

    private static int gfMul(int x, int y){
        int z = 0;
        for(int i = 7; i >= 0; i--){
            z = (z << 1) ^ ((z >>> 7) * 0x11D);
            z ^= ((y >>> i) & 1) * x;
        }
        return z;
    }

    // ================= drawing =================

    private void setFunction(int x, int y, boolean dark){
        modules[y][x] = dark;
        isFunction[y][x] = true;
    }

    private void drawFunctionPatterns(int ver){
        for(int i = 0; i < size; i++){
            setFunction(6, i, i % 2 == 0);
            setFunction(i, 6, i % 2 == 0);
        }

        drawFinder(3, 3);
        drawFinder(size - 4, 3);
        drawFinder(3, size - 4);

        int[] pos = alignmentPositions(ver);
        int n = pos.length;
        for(int i = 0; i < n; i++){
            for(int j = 0; j < n; j++){
                if(!((i == 0 && j == 0) || (i == 0 && j == n - 1) || (i == n - 1 && j == 0))){
                    drawAlignment(pos[i], pos[j]);
                }
            }
        }

        drawFormatBits(0);
        drawVersion(ver);
    }

    private int[] alignmentPositions(int ver){
        if(ver == 1) return new int[0];
        int numAlign = ver / 7 + 2;
        int step = (ver * 8 + numAlign * 3 + 5) / (numAlign * 4 - 4) * 2;
        int[] result = new int[numAlign];
        result[0] = 6;
        for(int i = numAlign - 1, p = size - 7; i >= 1; i--, p -= step) result[i] = p;
        return result;
    }

    private void drawFinder(int cx, int cy){
        for(int dy = -4; dy <= 4; dy++){
            for(int dx = -4; dx <= 4; dx++){
                int dist = Math.max(Math.abs(dx), Math.abs(dy));
                int x = cx + dx, y = cy + dy;
                if(x >= 0 && x < size && y >= 0 && y < size) setFunction(x, y, dist != 2 && dist != 4);
            }
        }
    }

    private void drawAlignment(int cx, int cy){
        for(int dy = -2; dy <= 2; dy++){
            for(int dx = -2; dx <= 2; dx++){
                setFunction(cx + dx, cy + dy, Math.max(Math.abs(dx), Math.abs(dy)) != 1);
            }
        }
    }

    private void drawFormatBits(int mask){
        int data = (0 << 3) | mask;  // ECC level M = 0
        int rem = data;
        for(int i = 0; i < 10; i++) rem = (rem << 1) ^ ((rem >>> 9) * 0x537);
        int bits = ((data << 10) | rem) ^ 0x5412;

        for(int i = 0; i <= 5; i++) setFunction(8, i, bit(bits, i));
        setFunction(8, 7, bit(bits, 6));
        setFunction(8, 8, bit(bits, 7));
        setFunction(7, 8, bit(bits, 8));
        for(int i = 9; i < 15; i++) setFunction(14 - i, 8, bit(bits, i));

        for(int i = 0; i < 8; i++) setFunction(size - 1 - i, 8, bit(bits, i));
        for(int i = 8; i < 15; i++) setFunction(8, size - 15 + i, bit(bits, i));
        setFunction(8, size - 8, true);
    }

    private void drawVersion(int ver){
        if(ver < 7) return;
        int rem = ver;
        for(int i = 0; i < 12; i++) rem = (rem << 1) ^ ((rem >>> 11) * 0x1F25);
        int bits = (ver << 12) | rem;
        for(int i = 0; i < 18; i++){
            boolean color = bit(bits, i);
            int a = size - 11 + i % 3;
            int b = i / 3;
            setFunction(a, b, color);
            setFunction(b, a, color);
        }
    }

    private void drawCodewords(byte[] data){
        int i = 0;
        for(int right = size - 1; right >= 1; right -= 2){
            if(right == 6) right = 5;
            for(int vert = 0; vert < size; vert++){
                for(int j = 0; j < 2; j++){
                    int x = right - j;
                    boolean upward = ((right + 1) & 2) == 0;
                    int y = upward ? size - 1 - vert : vert;
                    if(!isFunction[y][x] && i < data.length * 8){
                        modules[y][x] = bit(data[i >>> 3], 7 - (i & 7));
                        i++;
                    }
                }
            }
        }
    }

    private void applyMask(int mask){
        for(int y = 0; y < size; y++){
            for(int x = 0; x < size; x++){
                boolean invert;
                switch(mask){
                    case 0: invert = (x + y) % 2 == 0; break;
                    case 1: invert = y % 2 == 0; break;
                    case 2: invert = x % 3 == 0; break;
                    case 3: invert = (x + y) % 3 == 0; break;
                    case 4: invert = (x / 3 + y / 2) % 2 == 0; break;
                    case 5: invert = x * y % 2 + x * y % 3 == 0; break;
                    case 6: invert = (x * y % 2 + x * y % 3) % 2 == 0; break;
                    default: invert = ((x + y) % 2 + x * y % 3) % 2 == 0; break;
                }
                if(!isFunction[y][x] && invert) modules[y][x] = !modules[y][x];
            }
        }
    }

    private int penalty(){
        int result = 0;

        // rule 1: runs of 5+ same colour
        for(int pass = 0; pass < 2; pass++){
            for(int a = 0; a < size; a++){
                int run = 1;
                for(int b = 1; b < size; b++){
                    boolean cur = pass == 0 ? modules[a][b] : modules[b][a];
                    boolean prev = pass == 0 ? modules[a][b - 1] : modules[b - 1][a];
                    if(cur == prev){
                        run++;
                        if(run == 5) result += 3; else if(run > 5) result++;
                    } else run = 1;
                }
            }
        }

        // rule 2: 2x2 blocks
        for(int y = 0; y < size - 1; y++){
            for(int x = 0; x < size - 1; x++){
                boolean c = modules[y][x];
                if(c == modules[y][x + 1] && c == modules[y + 1][x] && c == modules[y + 1][x + 1]) result += 3;
            }
        }

        // rule 4: dark/light balance
        int dark = 0;
        for(boolean[] row : modules) for(boolean m : row) if(m) dark++;
        int total = size * size;
        int k = (Math.abs(dark * 20 - total * 10) + total - 1) / total - 1;
        result += k * 10;

        return result;
    }

    private static boolean bit(int x, int i){
        return ((x >>> i) & 1) != 0;
    }
}
