package is.pig.minecraft.admin.math;

/**
 * Pure Java record for 3D math operations.
 */
public record Vector3(double x, double y, double z) {
    
    public double distanceToSqr(Vector3 other) {
        double dx = this.x - other.x;
        double dy = this.y - other.y;
        double dz = this.z - other.z;
        return dx * dx + dy * dy + dz * dz;
    }

    public Vector3 subtract(Vector3 other) {
        return new Vector3(this.x - other.x, this.y - other.y, this.z - other.z);
    }

    public Vector3 normalize() {
        double length = Math.sqrt(x * x + y * y + z * z);
        if (length < 1.0E-4D) {
            return new Vector3(0, 0, 0);
        }
        return new Vector3(x / length, y / length, z / length);
    }

    public double dot(Vector3 other) {
        return this.x * other.x + this.y * other.y + this.z * other.z;
    }
}
