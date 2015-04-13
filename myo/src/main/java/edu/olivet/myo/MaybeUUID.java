package edu.olivet.myo;

import java.math.*;
import java.util.*;

import org.thingml.bglib.gui.*;

public class MaybeUUID {
	public final byte[] uuid;
	public final int hashCode;

	public MaybeUUID(byte... uuid) {
		this.uuid = uuid;
		hashCode = Arrays.hashCode(this.uuid);
	}

	public MaybeUUID(UUID uuid) {
		long longOne = uuid.getMostSignificantBits();
		long longTwo = uuid.getLeastSignificantBits();
		this.uuid = new byte[] { (byte) (longOne >>> 56), (byte) (longOne >>> 48), (byte) (longOne >>> 40), (byte) (longOne >>> 32),
		                         (byte) (longOne >>> 24), (byte) (longOne >>> 16), (byte) (longOne >>> 8), (byte) longOne,
		                         (byte) (longTwo >>> 56), (byte) (longTwo >>> 48), (byte) (longTwo >>> 40), (byte) (longTwo >>> 32),
		                         (byte) (longTwo >>> 24), (byte) (longTwo >>> 16), (byte) (longTwo >>> 8), (byte) longTwo };
		hashCode = Arrays.hashCode(this.uuid);
	}

	public MaybeUUID(String uuid) {
		this(ByteUtils.bytesFromString(uuid.replace('-', ' ').trim()));
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public String toString() {
		if (uuid.length == 16)
			try {
		        long msb = 0;
		        long lsb = 0;
		        for (int i=0; i<8; i++)
		            msb = (msb << 8) | (uuid[i] & 0xff);
		        for (int i=8; i<16; i++)
		            lsb = (lsb << 8) | (uuid[i] & 0xff);
		        return new UUID(msb, lsb).toString();
			} catch (Exception ex) { }
		return "0x" + new BigInteger(1, uuid).toString(16);
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof MaybeUUID))
			return false;
		MaybeUUID m = (MaybeUUID) o;
		return hashCode == m.hashCode && Arrays.equals(uuid, m.uuid);
	}
}
