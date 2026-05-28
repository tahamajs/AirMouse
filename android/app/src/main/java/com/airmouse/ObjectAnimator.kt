private fun animateToPosition(index: Int) {
    val pos = positions[index]
    val targetRotation = pos.rotation
    val targetRotationX = pos.rotationX
    val targetScaleX = pos.scaleX

    val animSet = AnimatorSet()
    animSet.playTogether(
        ObjectAnimator.ofFloat(phoneAnimation, "rotation", phoneAnimation.rotation, targetRotation),
        ObjectAnimator.ofFloat(phoneAnimation, "rotationX", phoneAnimation.rotationX, targetRotationX),
        ObjectAnimator.ofFloat(phoneAnimation, "scaleX", phoneAnimation.scaleX, targetScaleX),
        ObjectAnimator.ofFloat(phoneAnimation, "scaleY", phoneAnimation.scaleY, targetScaleX)   // keep aspect
    )
    animSet.duration = 600
    animSet.start()
}