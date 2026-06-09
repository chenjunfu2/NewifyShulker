package chenjunfu2.newifyshulker.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(ShulkerBoxBlock.class)
abstract class ShulkerBoxBlockMixin
{
	@WrapOperation(
		method = "onBreak",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/block/entity/BlockEntity;setStackNbt(Lnet/minecraft/item/ItemStack;)V"
		)
	)
	private void wrapSetStackNbt(BlockEntity instance, ItemStack stack, Operation<Void> original)//创造模式破坏带有物品的潜影盒
	{
		//添加物品
		original.call(instance, stack);
		
		//添加后，内部会存在id段，主动删除
		var itemTag = stack.getNbt();
		if(itemTag != null && itemTag.contains("BlockEntityTag", NbtElement.COMPOUND_TYPE))
		{
			itemTag.getCompound("BlockEntityTag").remove("id");
		}
		
		//因为只有Items非空才会进来，所以不用处理空的情况
	}
	
	
	@ModifyReturnValue(
		method = "getDroppedStacks",
		at = @At(
			value = "RETURN"
		)
	)
	private List<ItemStack> modifyDroppedStacks(List<ItemStack> original)//生存模式、方块、实体等破坏潜影盒
	{
		//确保不为空且仅含1个物品
		if (original == null || original.size() != 1)
		{
			return original;
		}
		
		//拿到返回值
		var item = original.get(0);
		if (item == null)
		{
			return original;
		}
		
		//获取掉落物的itemTag
		var itemTag = item.getNbt();
		
		//必须存在且类型正确
		if(itemTag == null || !itemTag.contains("BlockEntityTag", NbtElement.COMPOUND_TYPE))
		{
			return original;
		}
		
		//获取方块实体tag
		var tagBETag = itemTag.getCompound("BlockEntityTag");
		
		//首先移除id
		tagBETag.remove("id");
		
		//如果没有物品，那么移除物品
		do
		{
			if (!tagBETag.contains("Items", NbtElement.LIST_TYPE))
			{
				break;
			}
			
			//上面已经验证过，这里不可能返回null
			var items = (NbtList)tagBETag.get("Items");
			
			//非空退出
			if(!items.isEmpty())
			{
				break;
			}
			
			//移除空物品段
			tagBETag.remove("Items");
		}while(false);
		
		
		//如果移除后啥tagBETag都没了，那么把BlockEntityTag也删除，不要留下空的BlockEntityTag
		if(tagBETag.isEmpty())
		{
			itemTag.remove("BlockEntityTag");
		}
		
		//如果移除BlockEntityTag之后itemTag里啥都没了，那么把itemTag设为null，不要留下空（但是非null）的itemTag
		if(itemTag.isEmpty())
		{
			item.setNbt(null);//需要设置item内部的itemTag，而不是单纯的赋值itemTag为null
		}
		
		return original;
	}
}