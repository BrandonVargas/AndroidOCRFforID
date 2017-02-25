package mx.brandonvargas.ocrforid.util;

/**
 * Created by brandonvargas on 15/04/16.
 */
public class Percentage {
    public float x;
    public float y;
    public float width;
    public float height;

    public Percentage(float x, float y, float width, float height) {
        this.height = height;
        this.width = width;
        this.x = x;
        this.y = y;
    }

    public float getHeight() {
        return height;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    public float getWidth() {
        return width;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }
}
