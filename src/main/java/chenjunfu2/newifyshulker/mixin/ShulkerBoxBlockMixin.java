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
	private  void wrapSetStackNbt(BlockEntity instance, ItemStack stack, Operation<Void> original)
	{
		//添加物品
		original.call(instance, stack);
		
		//添加后，内部会存在id段，主动删除
		var nbt = stack.getNbt();
		if(nbt != null && nbt.contains("BlockEntityTag", NbtElement.COMPOUND_TYPE))
		{
			nbt.getCompound("BlockEntityTag").remove("id");
		}
		
		//因为只有Items非空才会进来，所以不用处理空的情况
	}
	
	
	@ModifyReturnValue(
		method = "getDroppedStacks",
		at = @At(
			value = "RETURN"
		)
	)
	private List<ItemStack> modifyDroppedStacks(List<ItemStack> original)
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
		
		//获取掉落物的nbt
		var nbt = item.getNbt();
		
		//必须存在且类型正确
		if(nbt == null || !nbt.contains("BlockEntityTag", NbtElement.COMPOUND_TYPE))
		{
			return original;
		}
		
		//获取方块实体tag
		var tag = nbt.getCompound("BlockEntityTag");
		
		//首先移除id
		tag.remove("id");
		
		//如果没有物品，那么移除物品
		do
		{
			if (!tag.contains("Items", NbtElement.LIST_TYPE))
			{
				break;
			}
			
			//上面已经验证过，这里不可能返回null
			var items = (NbtList)tag.get("Items");
			
			//非空退出
			if(!items.isEmpty())
			{
				break;
			}
			
			//移除空物品段
			tag.remove("Items");
		}while(false);
		
		
		//如果移除后啥tag都没了，那么把tag也删除
		if(tag.isEmpty())
		{
			nbt.remove("BlockEntityTag");
		}
		
		return original;
	}
}