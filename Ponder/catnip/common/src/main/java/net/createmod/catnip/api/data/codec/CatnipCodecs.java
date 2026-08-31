package net.createmod.catnip.api.data.codec;

import java.util.List;
import java.util.Set;
import java.util.stream.DoubleStream;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.PrimitiveCodec;

import net.minecraft.world.phys.AABB;

public interface CatnipCodecs {
	PrimitiveCodec<Character> CHAR = new PrimitiveCodec<>() {
		@Override
		public <T> DataResult<Character> read(final DynamicOps<T> ops, final T input) {
			return ops.getNumberValue(input)
				.map(n -> (char) n.intValue());
		}

		@Override
		public <T> T write(final DynamicOps<T> ops, final Character value) {
			return ops.createInt(value);
		}

		@Override
		public String toString() {
			return "Char";
		}
	};

	PrimitiveCodec<DoubleStream> DOUBLE_STREAM = new PrimitiveCodec<>() {
		@Override
		public <T> DataResult<DoubleStream> read(final DynamicOps<T> ops, final T input) {
			return ops.getStream(input).flatMap(stream -> {
				final List<T> list = stream.toList();
				if (list.stream().allMatch(element -> ops.getNumberValue(element).isSuccess())) {
					return DataResult.success(list.stream().mapToDouble(element -> ops.getNumberValue(element).getOrThrow().doubleValue()));
				}
				return DataResult.error(() -> "Some elements are not doubles: " + input);
			});
		}

		@Override
		public <T> T write(final DynamicOps<T> ops, final DoubleStream value) {
			return ops.createList(value.mapToObj(ops::createDouble));
		}

		@Override
		public String toString() {
			return "DoubleStream";
		}
	};

	static <E> Codec<Set<E>> set(Codec<E> codec) {
		return Codec.list(codec).xmap(Sets::newHashSet, Lists::newArrayList);
	}

	Codec<AABB> AABB_CODEC = DOUBLE_STREAM.comapFlatMap(
		stream -> ConversionUtil.fixedSize(stream, 3)
			.map(i -> new AABB(i[0], i[1], i[2], i[3], i[4], i[5])),
		i -> DoubleStream.of(i.minX, i.minY, i.minZ, i.maxX, i.maxY, i.maxZ)
	);
}
