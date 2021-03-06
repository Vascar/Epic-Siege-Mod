package funwayguy.esm.ai;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import funwayguy.esm.core.ESM_Settings;

public class ESM_EntityAIDigging extends EntityAIBase
{
	EntityLivingBase target;
	int[] markedLoc;
	EntityLiving entityDigger;
	int digTick = 0;
	int scanTick = 0;
	
	public ESM_EntityAIDigging(EntityLiving entity)
	{
		this.entityDigger = entity;
	}
	
	@Override
	public boolean shouldExecute()
	{
    	// Returns true if something like Iguana Tweaks is nerfing the vanilla picks. This will then cause zombies to ignore the harvestability of blocks when holding picks
    	boolean nerfedPick = !Items.iron_pickaxe.canHarvestBlock(Blocks.stone, new ItemStack(Items.iron_pickaxe));
		target = entityDigger.getAttackTarget();
		
		if(entityDigger.ticksExisted%10 == 0 && target != null && entityDigger.getNavigator().noPath() && entityDigger.getDistanceToEntity(target) > 1D && (target.onGround || !entityDigger.canEntityBeSeen(target)))
		{
			MovingObjectPosition mop = GetNextObstical(entityDigger, 2D);
			
			if(mop == null || mop.typeOfHit != MovingObjectType.BLOCK)
			{
				return false;
			}
			
			ItemStack item = entityDigger.getEquipmentInSlot(0);
			Block block = entityDigger.worldObj.getBlock(mop.blockX, mop.blockY, mop.blockZ);
			
			if(!ESM_Settings.ZombieDiggerTools || (item != null && (item.getItem().canHarvestBlock(block, item) || (item.getItem() instanceof ItemPickaxe && nerfedPick && block.getMaterial() == Material.rock))) || block.getMaterial().isToolNotRequired())
			{
				markedLoc = new int[]{mop.blockX, mop.blockY, mop.blockZ};
				return true;
			} else
			{
				return false;
			}
		}
		
		return false;
	}
	
	@Override
	public boolean continueExecuting()
	{
		return target != null && entityDigger != null && target.isEntityAlive() && entityDigger.isEntityAlive() && markedLoc != null && entityDigger.getNavigator().noPath() && entityDigger.getDistanceToEntity(target) > 1D && (target.onGround || !entityDigger.canEntityBeSeen(target));
	}
	
	@Override
	public void updateTask()
	{
    	// Returns true if something like Iguana Tweaks is nerfing the vanilla picks. This will then cause zombies to ignore the harvestability of blocks when holding picks
    	boolean nerfedPick = !Items.iron_pickaxe.canHarvestBlock(Blocks.stone, new ItemStack(Items.iron_pickaxe));
		
    	MovingObjectPosition mop = null;
    	
    	if(entityDigger.ticksExisted%10 == 0)
    	{
    		mop = GetNextObstical(entityDigger, 2D);
    	}
		
		if(mop != null && mop.typeOfHit == MovingObjectType.BLOCK)
		{
			markedLoc = new int[]{mop.blockX, mop.blockY, mop.blockZ};
		}
		
		if(markedLoc == null || markedLoc.length != 3 || entityDigger.worldObj.getBlock(markedLoc[0], markedLoc[1], markedLoc[2]) == Blocks.air)
		{
			digTick = 0;
			return;
		}
		
		Block block = entityDigger.worldObj.getBlock(markedLoc[0], markedLoc[1], markedLoc[2]);
		digTick++;
		
		float str = AIUtils.getBlockStrength(this.entityDigger, block, entityDigger.worldObj, markedLoc[0], markedLoc[1], markedLoc[2], !ESM_Settings.ZombieDiggerTools) * (digTick + 1);
		
		if(str >= 1F)
		{
			digTick = 0;
			
			ItemStack item = entityDigger.getEquipmentInSlot(0);
			boolean canHarvest = !ESM_Settings.ZombieDiggerTools || (item != null && (item.getItem().canHarvestBlock(block, item) || (item.getItem() instanceof ItemPickaxe && nerfedPick && block.getMaterial() == Material.rock))) || block.getMaterial().isToolNotRequired();
			entityDigger.worldObj.func_147480_a(markedLoc[0], markedLoc[1], markedLoc[2], canHarvest);
			markedLoc = null;
			entityDigger.getNavigator().setPath(entityDigger.getNavigator().getPathToEntityLiving(target), 1D);
		} else
		{
			if(digTick%5 == 0)
			{
				entityDigger.worldObj.playSoundAtEntity(entityDigger, block.stepSound.getStepResourcePath(), block.stepSound.getVolume() + 1F, block.stepSound.getPitch());
				entityDigger.swingItem();
				entityDigger.worldObj.destroyBlockInWorldPartially(entityDigger.getEntityId(), markedLoc[0], markedLoc[1], markedLoc[2], (int)(str * 10F));
			}
		}
	}
	
	@Override
	public void resetTask()
	{
		markedLoc = null;
		digTick = 0;
	}
	
	/**
	 * Rolls through all the points in the bounding box of the entity and raycasts them toward it's current heading to return any blocks that may be obstructing it's path.
	 * The bigger the entity the longer this calculation will take due to the increased number of points (Generic bipeds should only need 2)
	 */
    public MovingObjectPosition GetNextObstical(EntityLivingBase entityLiving, double dist)
    {
    	// Returns true if something like Iguana Tweaks is nerfing the vanilla picks. This will then cause zombies to ignore the harvestability of blocks when holding picks
    	boolean nerfedPick = !Items.iron_pickaxe.canHarvestBlock(Blocks.stone, new ItemStack(Items.iron_pickaxe));
        float f = 1.0F;
        float f1 = entityLiving.prevRotationPitch + (entityLiving.rotationPitch - entityLiving.prevRotationPitch) * f;
        float f2 = entityLiving.prevRotationYaw + (entityLiving.rotationYaw - entityLiving.prevRotationYaw) * f;
        
        int digWidth = MathHelper.ceiling_double_int(entityLiving.width);
        int digHeight = MathHelper.ceiling_double_int(entityLiving.height);
        
        int passMax = digWidth * digWidth * digHeight;
        
        int x = scanTick%digWidth - (digWidth/2);
        int y = scanTick/(digWidth * digWidth);
        int z = (scanTick%(digWidth * digWidth))/digWidth - (digWidth/2);
        
		double rayX = x + entityLiving.posX;
		double rayY = y + entityLiving.posY;
		double rayZ = z + entityLiving.posZ;
		
    	MovingObjectPosition mop = AIUtils.RayCastBlocks(entityLiving.worldObj, rayX, rayY, rayZ, f2, f1, dist, false);
    	
    	if(mop != null && mop.typeOfHit == MovingObjectType.BLOCK)
    	{
    		Block block = entityLiving.worldObj.getBlock(mop.blockX, mop.blockY, mop.blockZ);
    		int meta = entityLiving.worldObj.getBlockMetadata(mop.blockX, mop.blockY, mop.blockZ);
    		ItemStack item = entityLiving.getEquipmentInSlot(0);
    		
    		if(ESM_Settings.ZombieDigBlacklist.contains(Block.blockRegistry.getNameForObject(block)) == !ESM_Settings.ZombieSwapList || ESM_Settings.ZombieDigBlacklist.contains(Block.blockRegistry.getNameForObject(block) + ":" + meta) == !ESM_Settings.ZombieSwapList)
    		{
    			scanTick = (scanTick + 1)%passMax;
    			return null;
    		}
    		
    		if(!ESM_Settings.ZombieDiggerTools || (item != null && (item.getItem().canHarvestBlock(block, item) || (item.getItem() instanceof ItemPickaxe && nerfedPick && block.getMaterial() == Material.rock))) || block.getMaterial().isToolNotRequired())
    		{
    			scanTick = 0;
    			return mop;
    		} else
    		{
    			scanTick = (scanTick + 1)%passMax;
    			return null;
    		}
    	} else
    	{
			scanTick = (scanTick + 1)%passMax;
			return null;
    	}
    }
}
