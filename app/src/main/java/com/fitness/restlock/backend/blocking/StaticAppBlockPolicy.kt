package com.fitness.restlock.backend.blocking

class StaticAppBlockPolicy(
    private val exemptPackages: Set<String>,
) : AppBlockPolicy {
    override fun isPackageExempt(packageName: String): Boolean {
        return packageName in exemptPackages
    }
}
