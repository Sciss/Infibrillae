package de.sciss.infibrillae.geom

// This is an adapted Scala translation of the Path2D Java class of OpenJDK
// as released under GNU GPL 2 -- see original file header below.
// So it can be used in Scala.js.

/*
 * Copyright (c) 1996, 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/**
  * The `Shape` interface provides definitions for objects
  * that represent some form of geometric shape.  The `Shape`
  * is described by a `PathIterator` object, which can express the
  * outline of the `Shape` as well as a rule for determining
  * how the outline divides the 2D plane into interior and exterior
  * points.  Each `Shape` object provides callbacks to get the
  * bounding box of the geometry, determine whether points or
  * rectangles lie partly or entirely within the interior
  * of the `Shape`, and retrieve a `PathIterator`
  * object that describes the trajectory path of the `Shape`
  * outline.
  * <p>
  * <a id="def_insideness"><b>Definition of insideness:</b></a>
  * A point is considered to lie inside a
  * `Shape` if and only if:
  * <ul>
  * <li> it lies completely
  * inside the `Shape` boundary <i>or</i>
  * <li>
  * it lies exactly on the `Shape` boundary <i>and</i> the
  * space immediately adjacent to the
  * point in the increasing `X` direction is
  * entirely inside the boundary <i>or</i>
  * <li>
  * it lies exactly on a horizontal boundary segment <b>and</b> the
  * space immediately adjacent to the point in the
  * increasing `Y` direction is inside the boundary.
  * </ul>
  * <p>The `contains` and `intersects` methods
  * consider the interior of a `Shape` to be the area it
  * encloses as if it were filled.  This means that these methods
  * consider
  * unclosed shapes to be implicitly closed for the purpose of
  * determining if a shape contains or intersects a rectangle or if a
  * shape contains a point.
  *
  * @see java.awt.geom.PathIterator
  * @see java.awt.geom.AffineTransform
  * @see java.awt.geom.FlatteningPathIterator
  * @see java.awt.geom.GeneralPath
  * @author Jim Graham
  * @since 1.2
  */
trait Shape {
//  /**
//    * Returns an integer `Rectangle` that completely encloses the
//    * `Shape`.  Note that there is no guarantee that the
//    * returned `Rectangle` is the smallest bounding box that
//    * encloses the `Shape`, only that the `Shape`
//    * lies entirely within the indicated  `Rectangle`.  The
//    * returned `Rectangle` might also fail to completely
//    * enclose the `Shape` if the `Shape` overflows
//    * the limited range of the integer data type.  The
//    * `getBounds2D` method generally returns a
//    * tighter bounding box due to its greater flexibility in
//    * representation.
//    *
//    * <p>
//    * Note that the
//    * <a href="``/java.desktop/java/awt/Shape.html#def_insideness">
//    * definition of insideness</a> can lead to situations where points
//    * on the defining outline of the `shape` may not be considered
//    * contained in the returned `bounds` object, but only in cases
//    * where those points are also not considered contained in the original
//    * `shape`.
//    * </p>
//    * <p>
//    * If a `point` is inside the `shape` according to the
//    * `#` method, then
//    * it must be inside the returned `Rectangle` bounds object
//    * according to the `#`
//    * method of the `bounds`. Specifically:
//    * </p>
//    * <p>
//    * `shape.contains(x,y)` requires `bounds.contains(x,y)`
//    * </p>
//    * <p>
//    * If a `point` is not inside the `shape`, then it might
//    * still be contained in the `bounds` object:
//    * </p>
//    * <p>
//    * `bounds.contains(x,y)` does not imply `shape.contains(x,y)`
//    * </p>
//    *
//    * @return an integer `Rectangle` that completely encloses
//    *         the `Shape`.
//    * @see #getBounds2D
//    * @since 1.2
//    */
//  def getBounds: Rectangle

  /**
    * Returns a high precision and more accurate bounding box of
    * the `Shape` than the `getBounds` method.
    * Note that there is no guarantee that the returned
    * `Rectangle2D` is the smallest bounding box that encloses
    * the `Shape`, only that the `Shape` lies
    * entirely within the indicated `Rectangle2D`.  The
    * bounding box returned by this method is usually tighter than that
    * returned by the `getBounds` method and never fails due
    * to overflow problems since the return value can be an instance of
    * the `Rectangle2D` that uses double precision values to
    * store the dimensions.
    *
    * <p>
    * Note that the
    * <a href="``/java.desktop/java/awt/Shape.html#def_insideness">
    * definition of insideness</a> can lead to situations where points
    * on the defining outline of the `shape` may not be considered
    * contained in the returned `bounds` object, but only in cases
    * where those points are also not considered contained in the original
    * `shape`.
    * </p>
    * <p>
    * If a `point` is inside the `shape` according to the
    * `#` method, then it must
    * be inside the returned `Rectangle2D` bounds object according
    * to the `#` method of the
    * `bounds`. Specifically:
    * </p>
    * <p>
    * `shape.contains(p)` requires `bounds.contains(p)`
    * </p>
    * <p>
    * If a `point` is not inside the `shape`, then it might
    * still be contained in the `bounds` object:
    * </p>
    * <p>
    * `bounds.contains(p)` does not imply `shape.contains(p)`
    * </p>
    *
    * @return an instance of `Rectangle2D` that is a
    *         high-precision bounding box of the `Shape`.
    * @see #getBounds
    * @since 1.2
    */
  def getBounds2D: Rectangle2D

  /**
    * Tests if the specified coordinates are inside the boundary of the
    * `Shape`, as described by the
    * <a href="``/java.desktop/java/awt/Shape.html#def_insideness">
    * definition of insideness</a>.
    *
    * @param x the specified X coordinate to be tested
    * @param y the specified Y coordinate to be tested
    * @return `true` if the specified coordinates are inside
    *         the `Shape` boundary; `false`
    *         otherwise.
    * @since 1.2
    */
  def contains(x: Double, y: Double): Boolean

  /**
    * Tests if a specified `Point2D` is inside the boundary
    * of the `Shape`, as described by the
    * <a href="``/java.desktop/java/awt/Shape.html#def_insideness">
    * definition of insideness</a>.
    *
    * @param p the specified `Point2D` to be tested
    * @return `true` if the specified `Point2D` is
    *         inside the boundary of the `Shape`;
    *         `false` otherwise.
    * @since 1.2
    */
  def contains(p: Point2D): Boolean

  /**
    * Tests if the interior of the `Shape` intersects the
    * interior of a specified rectangular area.
    * The rectangular area is considered to intersect the `Shape`
    * if any point is contained in both the interior of the
    * `Shape` and the specified rectangular area.
    * <p>
    * The `Shape.intersects()` method allows a `Shape`
    * implementation to conservatively return `true` when:
    * <ul>
    * <li>
    * there is a high probability that the rectangular area and the
    * `Shape` intersect, but
    * <li>
    * the calculations to accurately determine this intersection
    * are prohibitively expensive.
    * </ul>
    * This means that for some `Shapes` this method might
    * return `true` even though the rectangular area does not
    * intersect the `Shape`.
    * The `java.awt.geom.Area` class performs
    * more accurate computations of geometric intersection than most
    * `Shape` objects and therefore can be used if a more precise
    * answer is required.
    *
    * @param x the X coordinate of the upper-left corner
    *          of the specified rectangular area
    * @param y the Y coordinate of the upper-left corner
    *          of the specified rectangular area
    * @param w the width of the specified rectangular area
    * @param h the height of the specified rectangular area
    * @return `true` if the interior of the `Shape` and
    *         the interior of the rectangular area intersect, or are
    *         both highly likely to intersect and intersection calculations
    *         would be too expensive to perform; `false` otherwise.
    * @see java.awt.geom.Area
    * @since 1.2
    */
  def intersects(x: Double, y: Double, w: Double, h: Double): Boolean

  /**
    * Tests if the interior of the `Shape` intersects the
    * interior of a specified `Rectangle2D`.
    * The `Shape.intersects()` method allows a `Shape`
    * implementation to conservatively return `true` when:
    * <ul>
    * <li>
    * there is a high probability that the `Rectangle2D` and the
    * `Shape` intersect, but
    * <li>
    * the calculations to accurately determine this intersection
    * are prohibitively expensive.
    * </ul>
    * This means that for some `Shapes` this method might
    * return `true` even though the `Rectangle2D` does not
    * intersect the `Shape`.
    * The `java.awt.geom.Area` class performs
    * more accurate computations of geometric intersection than most
    * `Shape` objects and therefore can be used if a more precise
    * answer is required.
    *
    * @param r the specified `Rectangle2D`
    * @return `true` if the interior of the `Shape` and
    *         the interior of the specified `Rectangle2D`
    *         intersect, or are both highly likely to intersect and intersection
    *         calculations would be too expensive to perform; `false`
    *         otherwise.
    * @see #intersects(double, double, double, double)
    * @since 1.2
    */
  def intersects(r: Rectangle2D): Boolean

  /**
    * Tests if the interior of the `Shape` entirely contains
    * the specified rectangular area.  All coordinates that lie inside
    * the rectangular area must lie within the `Shape` for the
    * entire rectangular area to be considered contained within the
    * `Shape`.
    * <p>
    * The `Shape.contains()` method allows a `Shape`
    * implementation to conservatively return `false` when:
    * <ul>
    * <li>
    * the `intersect` method returns `true` and
    * <li>
    * the calculations to determine whether or not the
    * `Shape` entirely contains the rectangular area are
    * prohibitively expensive.
    * </ul>
    * This means that for some `Shapes` this method might
    * return `false` even though the `Shape` contains
    * the rectangular area.
    * The `java.awt.geom.Area` class performs
    * more accurate geometric computations than most
    * `Shape` objects and therefore can be used if a more precise
    * answer is required.
    *
    * @param x the X coordinate of the upper-left corner
    *          of the specified rectangular area
    * @param y the Y coordinate of the upper-left corner
    *          of the specified rectangular area
    * @param w the width of the specified rectangular area
    * @param h the height of the specified rectangular area
    * @return `true` if the interior of the `Shape`
    *         entirely contains the specified rectangular area;
    *         `false` otherwise or, if the `Shape`
    *         contains the rectangular area and the
    *         `intersects` method returns `true`
    *         and the containment calculations would be too expensive to
    *         perform.
    * @see java.awt.geom.Area
    * @see #intersects
    * @since 1.2
    */
  def contains(x: Double, y: Double, w: Double, h: Double): Boolean

  /**
    * Tests if the interior of the `Shape` entirely contains the
    * specified `Rectangle2D`.
    * The `Shape.contains()` method allows a `Shape`
    * implementation to conservatively return `false` when:
    * <ul>
    * <li>
    * the `intersect` method returns `true` and
    * <li>
    * the calculations to determine whether or not the
    * `Shape` entirely contains the `Rectangle2D`
    * are prohibitively expensive.
    * </ul>
    * This means that for some `Shapes` this method might
    * return `false` even though the `Shape` contains
    * the `Rectangle2D`.
    * The `java.awt.geom.Area` class performs
    * more accurate geometric computations than most
    * `Shape` objects and therefore can be used if a more precise
    * answer is required.
    *
    * @param r The specified `Rectangle2D`
    * @return `true` if the interior of the `Shape`
    *         entirely contains the `Rectangle2D`;
    *         `false` otherwise or, if the `Shape`
    *         contains the `Rectangle2D` and the
    *         `intersects` method returns `true`
    *         and the containment calculations would be too expensive to
    *         perform.
    * @see #contains(double, double, double, double)
    * @since 1.2
    */
  def contains(r: Rectangle2D): Boolean

  /**
    * Returns an iterator object that iterates along the
    * `Shape` boundary and provides access to the geometry of the
    * `Shape` outline.  If an optional `AffineTransform`
    * is specified, the coordinates returned in the iteration are
    * transformed accordingly.
    * <p>
    * Each call to this method returns a fresh `PathIterator`
    * object that traverses the geometry of the `Shape` object
    * independently from any other `PathIterator` objects in use
    * at the same time.
    * <p>
    * It is recommended, but not guaranteed, that objects
    * implementing the `Shape` interface isolate iterations
    * that are in process from any changes that might occur to the original
    * object's geometry during such iterations.
    *
    * @param at an optional `AffineTransform` to be applied to the
    *           coordinates as they are returned in the iteration, or
    *           `null` if untransformed coordinates are desired
    * @return a new `PathIterator` object, which independently
    *         traverses the geometry of the `Shape`.
    * @since 1.2
    */
  def getPathIterator(at: AffineTransform): PathIterator

  /**
    * Returns an iterator object that iterates along the `Shape`
    * boundary and provides access to a flattened view of the
    * `Shape` outline geometry.
    * <p>
    * Only SEG_MOVETO, SEG_LINETO, and SEG_CLOSE point types are
    * returned by the iterator.
    * <p>
    * If an optional `AffineTransform` is specified,
    * the coordinates returned in the iteration are transformed
    * accordingly.
    * <p>
    * The amount of subdivision of the curved segments is controlled
    * by the `flatness` parameter, which specifies the
    * maximum distance that any point on the unflattened transformed
    * curve can deviate from the returned flattened path segments.
    * Note that a limit on the accuracy of the flattened path might be
    * silently imposed, causing very small flattening parameters to be
    * treated as larger values.  This limit, if there is one, is
    * defined by the particular implementation that is used.
    * <p>
    * Each call to this method returns a fresh `PathIterator`
    * object that traverses the `Shape` object geometry
    * independently from any other `PathIterator` objects in use at
    * the same time.
    * <p>
    * It is recommended, but not guaranteed, that objects
    * implementing the `Shape` interface isolate iterations
    * that are in process from any changes that might occur to the original
    * object's geometry during such iterations.
    *
    * @param at       an optional `AffineTransform` to be applied to the
    *                 coordinates as they are returned in the iteration, or
    *                 `null` if untransformed coordinates are desired
    * @param flatness the maximum distance that the line segments used to
    *                 approximate the curved segments are allowed to deviate
    *                 from any point on the original curve
    * @return a new `PathIterator` that independently traverses
    *         a flattened view of the geometry of the  `Shape`.
    * @since 1.2
    */
  def getPathIterator(at: AffineTransform, flatness: Double): PathIterator
}
