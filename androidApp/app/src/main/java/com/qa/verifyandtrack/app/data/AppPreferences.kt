package com.qa.verifyandtrack.app.data

import android.content.Context

object AppPreferences {
    private const val PREFS_NAME = "qa_prefs"
    private const val KEY_DELETE_BRANCHES = "delete_branches_single_pr"

    fun isDeleteBranchesEnabled(context: Context): Boolean {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_DELETE_BRANCHES, true)
    }

    fun setDeleteBranchesEnabled(context: Context, enabled: Boolean) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_DELETE_BRANCHES, enabled).apply()
    }
}
