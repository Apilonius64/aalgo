package tea.cs.data;

import java.awt.Rectangle;
import java.awt.event.MouseEvent;

// Multi purpose Vec2 class
public record Vec2 (double x, double y) {
    public record Tile (Vec2 size, Vec2 offset) {
        public boolean contains(Vec2 v) {
            return (
                v.x() > offset.x() && v.x() < (offset.x() + size.x()) &&
                v.y() > offset.y() && v.y() < (offset.y() + size.y())
            );
        }

        public static Tile of(Rectangle rect) {
            return new Tile(
                new Vec2(
                    (int)rect.getWidth(),
                    (int)rect.getHeight()
                ), new Vec2(
                    (int)rect.getMinX(),
                    (int)rect.getMinY()
                ));
        }
    }

    public Vec2 diff(Vec2 other) {
        return new Vec2(other.x - x, other.y - y);
    }

    public Vec2 sum(Vec2 other) {
        return new Vec2(other.x + x, other.y + y);
    }

    public Vec2 div(Vec2 other) {
        return new Vec2(x / other.x, y / other.y);
    }

    public Vec2 mul(Vec2 other) {
        return new Vec2(x * other.x, y * other.y);
    }

    public int dist(Vec2 other) {
        Vec2 a = diff(other);
        return (int)Math.floor(Math.sqrt(Math.pow(a.x(), 2) + Math.pow(a.y(), 2)));
    }

    public static Vec2 of(MouseEvent e) {
        return new Vec2(e.getX(), e.getY());
    }

    public static Vec2 ofCoord(Double lat, Double lon) {
        // Approx, Thanks: https://stackoverflow.com/a/24620800
        return new Vec2(
            lon*40000*Math.cos(Math.toRadians(lat))/360, // ?
            -lat*40000/360
        ).mul(new Vec2(10000)); // scale
    }

    public Vec2(double v) {
        this(v, v);
    }
}