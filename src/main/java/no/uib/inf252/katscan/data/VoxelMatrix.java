package no.uib.inf252.katscan.data;

import java.io.Serializable;
import java.util.Arrays;

/**
 * @author Marcelo Lima
 */
public class VoxelMatrix implements Serializable {

    private final int sizeX, sizeY, sizeZ;
    private final short[] grid;
    private final int[] histogram;
    private final float[] ratio;
    private int maxValue;

    public VoxelMatrix(int sizeZ, int sizeY, int sizeX) {
        if (sizeZ <= 0) throw new IllegalArgumentException("The size must be larger than zero, but Z was " + sizeZ);
        if (sizeY <= 0) throw new IllegalArgumentException("The size must be larger than zero, but Y was " + sizeY);
        if (sizeX <= 0) throw new IllegalArgumentException("The size must be larger than zero, but X was " + sizeX);

        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        grid = new short[sizeZ * sizeY * sizeX];
        histogram = new int[65536];
        float minSize = Math.min(sizeX, Math.min(sizeY, sizeZ));
        ratio = new float[] {sizeX / minSize, sizeY / minSize, sizeZ / minSize};
    }

    public int getSizeX() {
        return sizeX;
    }

    public int getSizeY() {
        return sizeY;
    }

    public int getSizeZ() {
        return sizeZ;
    }
    
    public int[] getHistogram() {
        int[] histogramReturn = new int[65536];
        System.arraycopy(histogram, 0, histogramReturn, 0, 65536);
        return histogramReturn;
    }

    public int getMaxValue() {
        return maxValue;
    }
    
    public void updateHistogram() {
        Arrays.fill(histogram, 0);
        maxValue = 0;
        for (int i = 0; i < grid.length; i++) {
            histogram[grid[i]]++;
            maxValue = Math.max(maxValue, grid[i]);
        }
    }

    public float[] getRatio() {
        return ratio;
    }
    
    public void setValue(int x, int y, int z, short value) {
        grid[z * sizeY * sizeX + y * sizeX + x] = value;
    }
    
    public short getValue(int x, int y, int z) {
        return grid[z * sizeY * sizeX + y * sizeX + x];
    }

    public short[] getValues() {
        return grid;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 89 * hash + Arrays.hashCode(this.grid);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final VoxelMatrix other = (VoxelMatrix) obj;
        if (!Arrays.equals(this.grid, other.grid)) {
            return false;
        }
        return true;
    }
    
}