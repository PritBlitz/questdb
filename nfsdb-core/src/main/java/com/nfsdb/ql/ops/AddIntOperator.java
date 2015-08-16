/*
 *  _  _ ___ ___     _ _
 * | \| | __/ __| __| | |__
 * | .` | _|\__ \/ _` | '_ \
 * |_|\_|_| |___/\__,_|_.__/
 *
 * Copyright (c) 2014-2015. The NFSdb project and its contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nfsdb.ql.ops;

import com.nfsdb.collections.ObjList;
import com.nfsdb.ql.Record;
import com.nfsdb.storage.ColumnType;

public class AddIntOperator extends AbstractBinaryOperator {

    public static final AddIntOperator FACTORY = new AddIntOperator();

    private AddIntOperator() {
        super(ColumnType.INT);
    }

    @Override
    public double getDouble(Record rec) {
        int l = lhs.getInt(rec);
        int r = rhs.getInt(rec);
        return l != Integer.MIN_VALUE && r != Integer.MIN_VALUE ? l + r : Double.NaN;
    }

    @Override
    public float getFloat(Record rec) {
        int l = lhs.getInt(rec);
        int r = rhs.getInt(rec);
        return l != Integer.MIN_VALUE && r != Integer.MIN_VALUE ? l + r : Float.NaN;
    }

    @Override
    public int getInt(Record rec) {
        int l = lhs.getInt(rec);
        int r = rhs.getInt(rec);
        return l != Integer.MIN_VALUE && r != Integer.MIN_VALUE ? l + r : Integer.MIN_VALUE;
    }

    @Override
    public long getLong(Record rec) {
        int l = lhs.getInt(rec);
        int r = rhs.getInt(rec);
        return l != Integer.MIN_VALUE && r != Integer.MIN_VALUE ? l + r : Long.MIN_VALUE;
    }

    @Override
    public Function newInstance(ObjList<VirtualColumn> args) {
        return new AddIntOperator();
    }
}
