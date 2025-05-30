package myGame;

/**
 * General static utility methods used by other systems.
 */
public final class Utility {
    private Utility() {} // Prevent instantiation

    public static double[] toDoubleArray(float[] array) {
        if (array == null) return null;
        double[] result = new double[array.length];
        for (int i = 0; i < array.length; i++)
            result[i] = array[i];
        return result;
    }

    public static float[] toFloatArray(double[] array) {
        if (array == null) return null;
        float[] result = new float[array.length];
        for (int i = 0; i < array.length; i++)
            result[i] = (float) array[i];
        return result;
    }
}
