package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.strategies.reinsert;

import java.util.Arrays;
import java.util.Collections;

import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.SpatialPrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.utilities.datastructures.ArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.pairs.DoubleIntPair;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2011
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * Reinsert objects on page overflow, starting with close objects first (even
 * when they will likely be inserted into the same page again!)
 * 
 * The strategy preferred by the R*-Tree
 * 
 * @author Erich Schubert
 */
@Reference(authors = "N. Beckmann, H.-P. Kriegel, R. Schneider, B. Seeger", title = "The R*-tree: an efficient and robust access method for points and rectangles", booktitle = "Proceedings of the 1990 ACM SIGMOD International Conference on Management of Data, Atlantic City, NJ, May 23-25, 1990", url = "http://dx.doi.org/10.1145/93597.98741")
public class CloseReinsert implements ReinsertStrategy {
  SpatialPrimitiveDistanceFunction<?, ? extends NumberDistance<?, ?>> distFunction = EuclideanDistanceFunction.STATIC;

  double share = 0.3;

  @Override
  public <E extends SpatialComparable, A> int[] computeReinserts(A entries, ArrayAdapter<E, A> getter, SpatialComparable page) {
    DoubleIntPair[] order = new DoubleIntPair[getter.size(entries)];
    for(int i = 0; i < order.length; i++) {
      double distance = distFunction.centerDistance(getter.get(entries, i), page).doubleValue();
      order[i] = new DoubleIntPair(distance, i);
    }
    Arrays.sort(order, Collections.reverseOrder());

    int num = (int) (share * order.length);
    int[] re = new int[num];
    for(int i = 0; i < num; i++) {
      re[i] = order[num - 1 - i].second;
    }
    return re;
  }
}