package util;

public class Either<L, R> {

    public boolean isLeft = false;
    public  boolean isRight = false;
    private L l = null;
    private R r = null;

    public Either(boolean isLeft, boolean isRight) {
        this.isLeft = isLeft;
        this.isRight = isRight;
    }

    public static <L> Either leftValue(L l) {
        Either e = new Either(true, false);
        e.l = l;
        return e;
    }

    public static <R> Either rightValue(R r) {
        Either e = new Either(false, true);
        e.r = r;
        return e;
    }

    public L getLeft() {
        return l;
    }

    public R getRight() {
        return r;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Either e = (Either) o;
        return isLeft == e.isLeft && isRight == e.isRight &&
                l == e.l && r == e.r;
    }
}
