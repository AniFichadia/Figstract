package com.anifichadia.figstract.importer.variable.model.variableorganization

/**
 * Only the last path segment is kept; all group structure is discarded.
 * `colour/Primary/Primary` → `["Primary"]`
 *
 * Note: this collapses all variables into the enclosing group, which may cause name collisions if the same leaf name
 * appears under different Figma groups.
 */
data object LeafOnlyVariableOrganizationStrategy : VariableOrganizationStrategy {
    override fun rewrite(figmaPath: String): List<String> =
        listOf(figmaPath.substringAfterLast(VariableOrganizationStrategy.DELIMITER))
}
