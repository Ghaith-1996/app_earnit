package com.fitness.restlock.backend.blocking

interface AppBlockPolicy {
    fun isPackageExempt(packageName: String): Boolean

    fun sanitizeAllowedPackages(packages: Set<String>): Set<String> {
        return packages
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .filterNot { isPackageExempt(it) }
            .toSet()
    }

    fun shouldBlock(
        foregroundPackage: String?,
        allowedPackages: Set<String>,
        lockActive: Boolean,
    ): Boolean {
        val packageName = foregroundPackage?.trim().orEmpty()
        return lockActive &&
            packageName.isNotEmpty() &&
            !isPackageExempt(packageName) &&
            packageName !in allowedPackages
    }
}
