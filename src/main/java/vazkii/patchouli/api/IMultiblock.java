package vazkii.patchouli.api;

import java.util.Collection;

import com.mojang.datafixers.util.Pair;
import net.minecraft.util.Identifier;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * An instance of a multiblock.
 * <br><br>
 * WARNING: This interface is provided only for usage with the API. For creating
 * an IMultiblock instance use the methods provided in the API main class. Please
 * do not create your own implementation of this, as it'll not be compatible with
 * all the features in the mod.
 */
public interface IMultiblock {

	// ================================================================================================
	// Builder methods
	// ================================================================================================
	
	/**
	 * Offsets the position of the multiblock by the amount specified.
	 * Works for both placement, validation, and rendering.
	 */
	public IMultiblock offset(int x, int y, int z);
	
	/**
	 * Offsets the view of the multiblock by the amount specified.
	 * Matters only for where the multiblock renders.
	 */
	public IMultiblock offsetView(int x, int y, int z);
	
	/**
	 * Sets the multiblock's symmetrical value. Symmetrical multiblocks
	 * check only in one rotation, not all 4. If your multiblock is symmetrical
	 * around the center axis, set this to true to prevent needless cycles.
	 */
	public IMultiblock setSymmetrical(boolean symmetrical);

	/**
	 * Sets the multiblock's ID. Not something you need to
	 * call yourself as the register method in the main API class does it for you.
	 */
	public IMultiblock setId(Identifier res);

	// ================================================================================================
	// Getters
	// ================================================================================================
	
	/**
	 * Gets if this multiblock is symmetrical.
	 * @see IMultiblock#setSymmetrical
	 */
	public boolean isSymmetrical();

	public Identifier getID();

	// ================================================================================================
	// Actual functionality
	// Note: DO NOT USE THESE METHODS IF YOUR MOD DOESN'T HAVE
	// A HARD DEPENDENCY ON PATCHOULI
	//
	// The stub API will return an empty multiblock that doesn't
	// do any of these things!
	// ================================================================================================
	
	/**
	 * Places the multiblock at the given position with the given rotation.
	 */
	public void place(World world, BlockPos pos, BlockRotation rotation);

	/**
	 * If this multiblock were anchored at world position {@code anchor} with rotation {@code rotation}, then
	 * return a pair whose first element is the final center position (after rotation and {@link #offset}),
	 * and whose second element describes each position of the multiblock.
	 *
	 * This is intended to be highly general, most of the other methods below are implemented in terms of this one.
	 * See the main Patchouli code to see what can be done with this.
	 */
	Pair<BlockPos, Collection<SimulateResult>> simulate(World world, BlockPos anchor, BlockRotation rotation, boolean forView);

	/**
	 * Validates if the multiblock exists at the given position. Will check all 4
	 * rotations if the multiblock is not symmetrical.
     * @return The rotation that worked, null if no match
	 */
	@Nullable
	public BlockRotation validate(World world, BlockPos pos);

	/**
	 * Validates the multiblock for a specific rotation
	 */
	public boolean validate(World world, BlockPos pos, BlockRotation rotation);

	/**
	 * Fine-grained check for whether any one given block of the multiblock exists at the given position
	 * with the given rotation.
	 * @param start The anchor position. The multiblock's {@link #offset} is not applied to this.
	 */
	public boolean test(World world, BlockPos start, int x, int y, int z, BlockRotation rotation);

	interface SimulateResult {
		/**
		 * Final world position this block will be matched or placed at
		 */
		public BlockPos getWorldPosition();

		/**
		 * The matcher used at this position
		 */
		public IStateMatcher getStateMatcher();

		/**
		 * The character used to express the state matcher, if this is a dense multiblock.
		 */
		@Nullable
		public Character getCharacter();

		/**
		 * @return Whether the multiblock is fulfilled at this position
		 */
		public boolean test(World world, BlockRotation rotation);
	}

}
