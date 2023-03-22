package core.utils;

import java.util.BitSet;

// Bit matrix used for cliff painting (and regions?)
public class BitMatrix {
    public BitSet[] matrix;
    public final int rows;
    public final int cols;

    public BitMatrix(int width, int heigth) {
        this.cols = width;
        this.rows = heigth;

        matrix = new BitSet[rows];

        for (int i = 0; i < rows; ++i) {
            matrix[i] = new BitSet(cols);
        }
    }
    
    public Boolean getBit(int x, int y) {
        if (0 <= x && x < cols && 0 <= y && y < rows) {
            return matrix[y].get(x);
        } else {
            return null;
        }
    }

    public void setBit(int x, int y, boolean value) {
        if (0 <= x && x < cols && 0 <= y && y < rows) {
            matrix[y].set(x, value);
        }
    }

    // for debug purposes
    public String asString() {
        String result = "\n";

        for (int i = 0; i < rows; ++i) {
            for (int j = 0; j < cols; ++j) {
                result += (getBit(i, j) ? "1" : "0");
            }
        }

        return result;
    }
}
