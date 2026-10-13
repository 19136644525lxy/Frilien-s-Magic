package yifei.frliliens.magic.client;

import org.joml.Vector3f;

import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 测地线球网格（基于正二十面体细分）。
 *
 * <p>除了生成球面三角形网格的顶点与边，还提供 {@link #getDualPolygons()}
 * 计算其对偶多面体（Goldberg 多面体）的面，由六边形和 12 个五边形组成，
 * 用于渲染护盾的六边形图案。
 */
public final class GeodesicSphere {

    /** 测地线球的所有顶点。 */
    public final List<Vector3f> vertices = new ArrayList<>();
    /** 测地线球的所有面（三角形，顶点索引）。 */
    public final List<Triangle> faces = new ArrayList<>();

    /**
     * @param radius       球半径
     * @param subdivisions 细分次数（0~3，越大越密）
     */
    public GeodesicSphere(float radius, int subdivisions) {
        float t = (1.0f + Mth.sqrt(5.0f)) / 2.0f; // 黄金比例

        addVertex(normalize(-1, t, 0, radius));
        addVertex(normalize(1, t, 0, radius));
        addVertex(normalize(-1, -t, 0, radius));
        addVertex(normalize(1, -t, 0, radius));
        addVertex(normalize(0, -1, t, radius));
        addVertex(normalize(0, 1, t, radius));
        addVertex(normalize(0, -1, -t, radius));
        addVertex(normalize(0, 1, -t, radius));
        addVertex(normalize(t, 0, -1, radius));
        addVertex(normalize(t, 0, 1, radius));
        addVertex(normalize(-t, 0, -1, radius));
        addVertex(normalize(-t, 0, 1, radius));

        faces.add(new Triangle(0, 11, 5));
        faces.add(new Triangle(0, 5, 1));
        faces.add(new Triangle(0, 1, 7));
        faces.add(new Triangle(0, 7, 10));
        faces.add(new Triangle(0, 10, 11));
        faces.add(new Triangle(1, 5, 9));
        faces.add(new Triangle(5, 11, 4));
        faces.add(new Triangle(11, 10, 2));
        faces.add(new Triangle(10, 7, 6));
        faces.add(new Triangle(7, 1, 8));
        faces.add(new Triangle(3, 9, 4));
        faces.add(new Triangle(3, 4, 2));
        faces.add(new Triangle(3, 2, 6));
        faces.add(new Triangle(3, 6, 8));
        faces.add(new Triangle(3, 8, 9));
        faces.add(new Triangle(4, 9, 5));
        faces.add(new Triangle(2, 4, 11));
        faces.add(new Triangle(6, 2, 10));
        faces.add(new Triangle(8, 6, 7));
        faces.add(new Triangle(9, 8, 1));

        for (int i = 0; i < subdivisions; i++) {
            List<Triangle> newFaces = new ArrayList<>();
            for (Triangle tri : faces) {
                Vector3f v1 = vertices.get(tri.v1);
                Vector3f v2 = vertices.get(tri.v2);
                Vector3f v3 = vertices.get(tri.v3);

                int ia = addVertex(normalize(midpoint(v1, v2), radius));
                int ib = addVertex(normalize(midpoint(v2, v3), radius));
                int ic = addVertex(normalize(midpoint(v3, v1), radius));

                newFaces.add(new Triangle(tri.v1, ia, ic));
                newFaces.add(new Triangle(tri.v2, ib, ia));
                newFaces.add(new Triangle(tri.v3, ic, ib));
                newFaces.add(new Triangle(ia, ib, ic));
            }
            faces.clear();
            faces.addAll(newFaces);
        }
    }

    private int addVertex(Vector3f v) {
        for (int i = 0; i < vertices.size(); i++) {
            if (vertices.get(i).distanceSquared(v) < 1.0e-8) {
                return i;
            }
        }
        vertices.add(v);
        return vertices.size() - 1;
    }

    private static Vector3f normalize(float x, float y, float z, float radius) {
        float len = Mth.sqrt(x * x + y * y + z * z);
        return new Vector3f(x / len * radius, y / len * radius, z / len * radius);
    }

    private static Vector3f normalize(Vector3f v, float radius) {
        return normalize(v.x, v.y, v.z, radius);
    }

    private static Vector3f midpoint(Vector3f a, Vector3f b) {
        return new Vector3f((a.x + b.x) * 0.5f, (a.y + b.y) * 0.5f, (a.z + b.z) * 0.5f);
    }

    /** 三角形面（顶点索引）。 */
    public record Triangle(int v1, int v2, int v3) {
    }

    /**
     * 计算对偶多面体的所有面（六边形 + 12 个五边形）。
     *
     * <p>对偶规则：测地线球的每个顶点变成对偶多面体的一个面中心，
     * 与该顶点相邻的所有三角形面的质心构成对偶面的顶点。
     *
     * @return 对偶面列表，每个面是顶点坐标列表（已按环绕顺序排列）
     */
    public List<List<Vector3f>> getDualPolygons() {
        List<List<Vector3f>> polygons = new ArrayList<>();

        for (int vi = 0; vi < vertices.size(); vi++) {
            Vector3f vertex = vertices.get(vi);
            // 收集包含该顶点的所有三角形
            List<Triangle> adjacent = new ArrayList<>();
            for (Triangle tri : faces) {
                if (tri.v1 == vi || tri.v2 == vi || tri.v3 == vi) {
                    adjacent.add(tri);
                }
            }
            if (adjacent.isEmpty()) {
                continue;
            }

            // 计算每个相邻三角形的质心
            List<Vector3f> centroids = new ArrayList<>();
            for (Triangle tri : adjacent) {
                Vector3f a = vertices.get(tri.v1);
                Vector3f b = vertices.get(tri.v2);
                Vector3f c = vertices.get(tri.v3);
                centroids.add(new Vector3f(
                        (a.x + b.x + c.x) / 3.0f,
                        (a.y + b.y + c.y) / 3.0f,
                        (a.z + b.z + c.z) / 3.0f));
            }

            // 按环绕顺序排列：投影到顶点的切平面上，按极角排序
            List<Vector3f> ordered = sortAroundVertex(vertex, centroids);
            polygons.add(ordered);
        }

        return polygons;
    }

    /**
     * 将一组点按围绕指定顶点的极角排序，形成环绕多边形。
     */
    private static List<Vector3f> sortAroundVertex(Vector3f center, List<Vector3f> points) {
        // 以 center 为原点建立切平面坐标系
        Vector3f normal = center.normalize(new Vector3f());
        // 任取一个不平行于 normal 的向量构造切线
        Vector3f ref = (Math.abs(normal.x) < 0.9f) ? new Vector3f(1, 0, 0) : new Vector3f(0, 1, 0);
        Vector3f tangent = new Vector3f(normal).cross(ref).normalize();
        Vector3f bitangent = new Vector3f(normal).cross(tangent).normalize();

        List<Vector3f> sorted = new ArrayList<>(points);
        Collections.sort(sorted, (p1, p2) -> {
            Vector3f d1 = new Vector3f(p1).sub(center);
            Vector3f d2 = new Vector3f(p2).sub(center);
            float a1 = (float) Math.atan2(d1.dot(bitangent), d1.dot(tangent));
            float a2 = (float) Math.atan2(d2.dot(bitangent), d2.dot(tangent));
            return Float.compare(a1, a2);
        });
        return sorted;
    }
}
