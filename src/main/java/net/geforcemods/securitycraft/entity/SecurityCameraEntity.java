package net.geforcemods.securitycraft.entity;

import net.geforcemods.securitycraft.ConfigHandler;
import net.geforcemods.securitycraft.SCContent;
import net.geforcemods.securitycraft.SecurityCraft;
import net.geforcemods.securitycraft.api.IModuleInventory;
import net.geforcemods.securitycraft.blocks.SecurityCameraBlock;
import net.geforcemods.securitycraft.misc.KeyBindings;
import net.geforcemods.securitycraft.misc.ModuleType;
import net.geforcemods.securitycraft.misc.SCSounds;
import net.geforcemods.securitycraft.network.client.SetPlayerPositionAndRotation;
import net.geforcemods.securitycraft.network.server.GiveNightVision;
import net.geforcemods.securitycraft.network.server.SetCameraPowered;
import net.geforcemods.securitycraft.network.server.SetCameraRotation;
import net.geforcemods.securitycraft.tileentity.SecurityCameraTileEntity;
import net.geforcemods.securitycraft.util.BlockUtils;
import net.geforcemods.securitycraft.util.PlayerUtils;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;
import net.minecraftforge.fml.network.PacketDistributor;

public class SecurityCameraEntity extends Entity{

	private final double CAMERA_SPEED = ConfigHandler.CLIENT.cameraSpeed.get();
	public int blockPosX;
	public int blockPosY;
	public int blockPosZ;
	private double cameraUseX;
	private double cameraUseY;
	private double cameraUseZ;
	private float cameraUseYaw;
	private float cameraUsePitch;
	private int id;
	public int screenshotSoundCooldown = 0;
	private int redstoneCooldown = 0;
	private int toggleNightVisionCooldown = 0;
	private int toggleLightCooldown = 0;
	private boolean shouldProvideNightVision = false;
	private float zoomAmount = 1F;
	private String playerViewingName = null;
	private boolean zooming = false;

	public SecurityCameraEntity(EntityType<SecurityCameraEntity> type, World world){
		super(SCContent.eTypeSecurityCamera, world);
		noClip = true;
	}

	public SecurityCameraEntity(World world, double x, double y, double z, int id, PlayerEntity player){
		this(SCContent.eTypeSecurityCamera, world);
		blockPosX = (int) x;
		blockPosY = (int) y;
		blockPosZ = (int) z;
		cameraUseX = player.posX;
		cameraUseY = player.posY;
		cameraUseZ = player.posZ;
		cameraUseYaw = player.rotationYaw;
		cameraUsePitch = player.rotationPitch;
		this.id = id;
		playerViewingName = player.getName().getFormattedText();
		setPosition(x + 0.5D, y, z + 0.5D);

		TileEntity te = world.getTileEntity(getPosition());

		if(te instanceof SecurityCameraTileEntity)
			setInitialPitchYaw((SecurityCameraTileEntity)te);
	}

	public SecurityCameraEntity(World world, double x, double y, double z, int id, SecurityCameraEntity camera){
		this(SCContent.eTypeSecurityCamera, world);
		blockPosX = (int) x;
		blockPosY = (int) y;
		blockPosZ = (int) z;
		cameraUseX = camera.cameraUseX;
		cameraUseY = camera.cameraUseY;
		cameraUseZ = camera.cameraUseZ;
		cameraUseYaw = camera.cameraUseYaw;
		cameraUsePitch = camera.cameraUsePitch;
		this.id = id;
		playerViewingName = camera.playerViewingName;
		setPosition(x + 0.5D, y, z + 0.5D);

		TileEntity te = world.getTileEntity(getPosition());

		if(te instanceof SecurityCameraTileEntity)
			setInitialPitchYaw((SecurityCameraTileEntity)te);
	}

	private void setInitialPitchYaw(SecurityCameraTileEntity te)
	{
		if(te != null && te.hasModule(ModuleType.SMART) && te.lastPitch != Float.MAX_VALUE && te.lastYaw != Float.MAX_VALUE)
		{
			rotationPitch = te.lastPitch;
			rotationYaw = te.lastYaw;
		}
		else
		{
			rotationPitch = 30F;

			Direction facing = world.getBlockState(BlockUtils.toPos((int) Math.floor(posX), (int) posY, (int) Math.floor(posZ))).get(SecurityCameraBlock.FACING);

			if(facing == Direction.NORTH)
				rotationYaw = 180F;
			else if(facing == Direction.WEST)
				rotationYaw = 90F;
			else if(facing == Direction.SOUTH)
				rotationYaw = 0F;
			else if(facing == Direction.EAST)
				rotationYaw = 270F;
			else if(facing == Direction.DOWN)
				rotationPitch = 75;
		}
	}

	@Override
	public double getMountedYOffset(){
		return -0.75D;
	}

	@Override
	protected boolean shouldSetPosAfterLoading(){
		return false;
	}

	@Override
	public boolean canBeRiddenInWater(Entity rider){
		return false;
	}

	@Override
	public void tick(){
		if(world.isRemote && isBeingRidden()){
			PlayerEntity lowestEntity = (PlayerEntity)getPassengers().get(0);

			if(lowestEntity != Minecraft.getInstance().player)
				return;

			if(screenshotSoundCooldown > 0)
				screenshotSoundCooldown -= 1;

			if(redstoneCooldown > 0)
				redstoneCooldown -= 1;

			if(toggleNightVisionCooldown > 0)
				toggleNightVisionCooldown -= 1;

			if(toggleLightCooldown > 0)
				toggleLightCooldown -= 1;

			if(lowestEntity.rotationYaw != rotationYaw){
				lowestEntity.setPositionAndRotation(lowestEntity.posX, lowestEntity.posY, lowestEntity.posZ, rotationYaw, rotationPitch);
				lowestEntity.rotationYaw = rotationYaw;
			}

			if(lowestEntity.rotationPitch != rotationPitch)
				lowestEntity.setPositionAndRotation(lowestEntity.posX, lowestEntity.posY, lowestEntity.posZ, rotationYaw, rotationPitch);

			checkKeysPressed();
			if(getPassengers().size() != 0 && shouldProvideNightVision)
				SecurityCraft.channel.sendToServer(new GiveNightVision());
		}

		if(!world.isRemote)
			if(getPassengers().size() == 0 || BlockUtils.getBlock(world, blockPosX, blockPosY, blockPosZ) != SCContent.SECURITY_CAMERA.get()){
				remove();
				return;
			}
	}

	private void checkKeysPressed() {
		if(Minecraft.getInstance().gameSettings.keyBindForward.isKeyDown())
			moveViewUp();

		if(Minecraft.getInstance().gameSettings.keyBindBack.isKeyDown())
			moveViewDown();

		if(Minecraft.getInstance().gameSettings.keyBindLeft.isKeyDown())
			moveViewLeft();

		if(Minecraft.getInstance().gameSettings.keyBindRight.isKeyDown())
			moveViewRight();

		if(KeyBindings.cameraEmitRedstone.isPressed() && redstoneCooldown == 0){
			setRedstonePower();
			redstoneCooldown = 30;
		}

		if(KeyBindings.cameraActivateNightVision.isPressed() && toggleNightVisionCooldown == 0)
			enableNightVision();

		if(KeyBindings.cameraZoomIn.isPressed())
		{
			zoomIn();
			zooming = true;
		}
		else if(KeyBindings.cameraZoomOut.isPressed())
		{
			zoomOut();
			zooming = true;
		}
		else
			zooming = false;
	}

	public void moveViewUp() {
		if(isCameraDown())
		{
			if(rotationPitch > 40F)
				setRotation(rotationYaw, rotationPitch -= CAMERA_SPEED);
		}
		else if(rotationPitch > -25F)
			setRotation(rotationYaw, rotationPitch -= CAMERA_SPEED);

		updateServerRotation();
	}

	public void moveViewDown(){
		if(isCameraDown())
		{
			if(rotationPitch < 100F)
				setRotation(rotationYaw, rotationPitch += CAMERA_SPEED);
		}
		else if(rotationPitch < 60F)
			setRotation(rotationYaw, rotationPitch += CAMERA_SPEED);

		updateServerRotation();
	}

	public void moveViewLeft() {
		BlockState state = world.getBlockState(BlockUtils.toPos((int) Math.floor(posX), (int) posY, (int) Math.floor(posZ)));

		if(state.has(SecurityCameraBlock.FACING)) {
			Direction facing = state.get(SecurityCameraBlock.FACING);

			if(facing == Direction.EAST)
			{
				if((rotationYaw - CAMERA_SPEED) > -180F)
					setRotation(rotationYaw -= CAMERA_SPEED, rotationPitch);
			}
			else if(facing == Direction.WEST)
			{
				if((rotationYaw - CAMERA_SPEED) > 0F)
					setRotation(rotationYaw -= CAMERA_SPEED, rotationPitch);
			}
			else if(facing == Direction.NORTH)
			{
				// Handles some problems the occurs from the way the rotationYaw value works in MC
				if((((rotationYaw - CAMERA_SPEED) > 90F) && ((rotationYaw - CAMERA_SPEED) < 185F)) || (((rotationYaw - CAMERA_SPEED) > -190F) && ((rotationYaw - CAMERA_SPEED) < -90F)))
					setRotation(rotationYaw -= CAMERA_SPEED, rotationPitch);
			}
			else if(facing == Direction.SOUTH)
			{
				if((rotationYaw - CAMERA_SPEED) > -90F)
					setRotation(rotationYaw -= CAMERA_SPEED, rotationPitch);
			}
			else if(facing == Direction.DOWN)
				setRotation(rotationYaw -= CAMERA_SPEED, rotationPitch);

			updateServerRotation();
		}
	}

	public void moveViewRight(){
		BlockState state = world.getBlockState(BlockUtils.toPos((int) Math.floor(posX), (int) posY, (int) Math.floor(posZ)));

		if(state.has(SecurityCameraBlock.FACING)) {
			Direction facing = state.get(SecurityCameraBlock.FACING);

			if(facing == Direction.EAST)
			{
				if((rotationYaw + CAMERA_SPEED) < 0F)
					setRotation(rotationYaw += CAMERA_SPEED, rotationPitch);
			}
			else if(facing == Direction.WEST)
			{
				if((rotationYaw + CAMERA_SPEED) < 180F)
					setRotation(rotationYaw += CAMERA_SPEED, rotationPitch);
			}
			else if(facing == Direction.NORTH)
			{
				if((((rotationYaw + CAMERA_SPEED) > 85F) && ((rotationYaw + CAMERA_SPEED) < 185F)) || ((rotationYaw + CAMERA_SPEED) < -95F) && ((rotationYaw + CAMERA_SPEED) > -180F))
					setRotation(rotationYaw += CAMERA_SPEED, rotationPitch);
			}
			else if(facing == Direction.SOUTH)
			{
				if((rotationYaw + CAMERA_SPEED) < 90F)
					setRotation(rotationYaw += CAMERA_SPEED, rotationPitch);
			}
			else if(facing == Direction.DOWN)
				setRotation(rotationYaw += CAMERA_SPEED, rotationPitch);

			updateServerRotation();
		}
	}

	public void zoomIn()
	{
		zoomAmount = Math.max(zoomAmount - 0.1F, 0.1F);

		if(!zooming)
			Minecraft.getInstance().world.playSound(getPosition(), SCSounds.CAMERAZOOMIN.event, SoundCategory.BLOCKS, 1.0F, 1.0F, true);
	}

	public void zoomOut()
	{
		zoomAmount = Math.min(zoomAmount + 0.1F, 1.5F);

		if(!zooming)
			Minecraft.getInstance().world.playSound(getPosition(), SCSounds.CAMERAZOOMIN.event, SoundCategory.BLOCKS, 1.0F, 1.0F, true);
	}

	public void setRedstonePower() {
		BlockPos pos = BlockUtils.toPos((int) Math.floor(posX), (int) posY, (int) Math.floor(posZ));

		if(((IModuleInventory) world.getTileEntity(pos)).hasModule(ModuleType.REDSTONE))
			SecurityCraft.channel.sendToServer(new SetCameraPowered(pos, !world.getBlockState(pos).get(SecurityCameraBlock.POWERED)));
	}

	public void enableNightVision() {
		toggleNightVisionCooldown = 30;
		shouldProvideNightVision = !shouldProvideNightVision;
	}

	public float getZoomAmount(){
		return zoomAmount;
	}

	private void updateServerRotation(){
		SecurityCraft.channel.sendToServer(new SetCameraRotation(rotationYaw, rotationPitch));
	}

	private boolean isCameraDown()
	{
		return world.getTileEntity(getPosition()) instanceof SecurityCameraTileEntity && ((SecurityCameraTileEntity)world.getTileEntity(getPosition())).down;
	}

	@Override
	public void remove(){
		super.remove();

		if(playerViewingName != null && PlayerUtils.isPlayerOnline(playerViewingName)){
			PlayerEntity player = PlayerUtils.getPlayerFromName(playerViewingName);
			player.setPositionAndUpdate(cameraUseX, cameraUseY, cameraUseZ);
			SecurityCraft.channel.send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity)player), new SetPlayerPositionAndRotation(cameraUseX, cameraUseY, cameraUseZ, cameraUseYaw, cameraUsePitch));
		}
	}

	public BlockPos getPreviousPlayerPos()
	{
		return new BlockPos(cameraUseX, cameraUseY, cameraUseZ);
	}

	@Override
	protected void registerData(){}

	@Override
	public void writeAdditional(CompoundNBT tag){
		tag.putInt("CameraID", id);
		tag.putString("playerName", playerViewingName);
		tag.putDouble("cameraUseX", cameraUseX);
		tag.putDouble("cameraUseY", cameraUseY);
		tag.putDouble("cameraUseZ", cameraUseZ);
		tag.putDouble("cameraUseYaw", cameraUseYaw);
		tag.putDouble("cameraUsePitch", cameraUsePitch);
	}

	@Override
	public void readAdditional(CompoundNBT tag){
		id = tag.getInt("CameraID");
		playerViewingName = tag.getString("playerName");
		cameraUseX = tag.getDouble("cameraUseX");
		cameraUseY = tag.getDouble("cameraUseY");
		cameraUseZ = tag.getDouble("cameraUseZ");
		cameraUseYaw = tag.getFloat("cameraUseYaw");
		cameraUsePitch = tag.getFloat("cameraUsePitch");
	}

	@Override
	public IPacket<?> createSpawnPacket()
	{
		return NetworkHooks.getEntitySpawningPacket(this);
	}
}