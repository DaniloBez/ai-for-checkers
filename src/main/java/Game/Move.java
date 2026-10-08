package Game;

public record Move(
        int fromIndex,
        int toIndex,
        int capturedMask,
        boolean isPromotion
) {

}
