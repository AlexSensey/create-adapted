package net.createmod.catnip.api.data.codec;

import java.util.Arrays;
import java.util.function.Supplier;
import java.util.stream.DoubleStream;

import com.mojang.serialization.DataResult;

public class ConversionUtil {
	public static DataResult<double[]> fixedSize(DoubleStream stream, int size) {
		double[] array = stream.limit(size + 1).toArray();
		if (array.length != size) {
			Supplier<String> supplier = () -> "Input is not a list of " + size + " doubles";
			return array.length >= size ? DataResult.error(supplier, Arrays.copyOf(array, size)) : DataResult.error(supplier);
		} else {
			return DataResult.success(array);
		}
	}
}
