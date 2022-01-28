public class Test {



    private static long sequenceDistance(long from, long to)
    {
        int _shift = (8 - 2) * 8;

        to <<= _shift;
        from <<= _shift;

        return ((long)(from - to)) >> _shift;
    }

    public static void main(String[] args) {
        System.out.println((short) (Short.MAX_VALUE + 1));
    }

}
