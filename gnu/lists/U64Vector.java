// This file is generated from PrimVector.template. DO NOT EDIT! 
// Copyright (c) 2001, 2002, 2015  Per M.A. Bothner and Brainfood Inc.
// This is free software;  for terms and warranty disclaimer see ./COPYING.

package gnu.lists;
import java.io.*;
import gnu.math.ULong;

/** Simple adjustable-length vector of unsigned 64-bit integers (longs). */

public  class U64Vector extends LongVector<ULong>
{
    public U64Vector() {
        data = empty;
    }

    public U64Vector(int size, long value) {
        long[] array = new long[size];
        data = array;
        if (value != 0) {
            while (--size >= 0)
                array[size] = value;
        }
    }

    public U64Vector(int size) {
        this(new long[size]);
    }

    /** Reuses the argument without making a copy. */
    public U64Vector(long[] data) {
        this.data = data;
    }


    /** Makes a copy of (part of) the argument array. */
    public U64Vector(long[] values, int offset, int length) {
        this(length);
        System.arraycopy(values, offset, data, 0, length);
    }

    public final ULong get(int index) {
        return ULong.valueOf(data[effectiveIndex(index)]);
    }

    public final ULong getRaw(int index) {
        return ULong.valueOf(data[index]);
    }

    @Override
    public final void setRaw(int index, ULong value) {
        data[index] = value.longValue();
    }

    @Override
    protected U64Vector newInstance(int newLength) {
        return new U64Vector(newLength < 0 ? data : new long[newLength]);
    }

    public static U64Vector castOrNull(Object obj) {
        if (obj instanceof long[])
            return new U64Vector((long[]) obj);
        if (obj instanceof U64Vector)
            return (U64Vector) obj;
        return null;
    }

    public static U64Vector cast(Object value) {
        U64Vector vec = castOrNull(value);
        if (vec == null) {
            String msg;
            if (value == null)
                msg = "cannot convert null to U64Vector";
            else
                msg = "cannot convert a "+value.getClass().getName()+" to U64Vector";
            throw new ClassCastException(msg);
        }
        return vec;
    }
    public int getElementKind() { return INT_U64_VALUE; }

    public String getTag() { return "u64"; }

    public void consumePosRange(int iposStart, int iposEnd, Consumer out) {
        if (out.ignoring())
            return;
        int i = nextIndex(iposStart);
        int end = nextIndex(iposEnd);
        for (;  i < end;  i++)
            Sequences.writeULong(getLong(i), out);
    }

    public int compareTo(Object obj) {
        U64Vector vec2 = (U64Vector) obj;
        long[] arr1 = data;
        long[] arr2 = vec2.data;
        int n1 = size();
        int n2 = vec2.size();
        int n = n1 > n2 ? n2 : n1;
        for (int i = 0;  i < n;  i++) {
            long v1 = arr1[effectiveIndex(i)];
            long v2 = arr2[effectiveIndex(i)];
            if (v1 != v2)
                return v1 > v2 ? 1 : -1;
        }
        return n1 - n2;
    }

}
