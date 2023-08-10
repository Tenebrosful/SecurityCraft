package net.geforcemods.securitycraft.network.client;

import java.util.function.Supplier;

import net.geforcemods.securitycraft.blockentities.UsernameLoggerBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.network.NetworkEvent;

public class UpdateLogger {
	private int x, y, z, i;
	private String username;
	private String uuid;
	private long timestamp;

	public UpdateLogger() {}

	public UpdateLogger(int x, int y, int z, int i, String username, String uuid, long timestamp) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.i = i;
		this.username = username;
		this.uuid = uuid;
		this.timestamp = timestamp;
	}

	public UpdateLogger(PacketBuffer buf) {
		x = buf.readInt();
		y = buf.readInt();
		z = buf.readInt();
		i = buf.readInt();
		username = buf.readUtf(Integer.MAX_VALUE / 4);
		uuid = buf.readUtf(Integer.MAX_VALUE / 4);
		timestamp = buf.readLong();
	}

	public void encode(PacketBuffer buf) {
		buf.writeInt(x);
		buf.writeInt(y);
		buf.writeInt(z);
		buf.writeInt(i);
		buf.writeUtf(username);
		buf.writeUtf(uuid);
		buf.writeLong(timestamp);
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		BlockPos pos = new BlockPos(x, y, z);

		UsernameLoggerBlockEntity te = (UsernameLoggerBlockEntity) Minecraft.getInstance().player.level.getBlockEntity(pos);

		if (te != null) {
			te.getPlayers()[i] = username;
			te.getUuids()[i] = uuid;
			te.getTimestamps()[i] = timestamp;
		}
	}
}
