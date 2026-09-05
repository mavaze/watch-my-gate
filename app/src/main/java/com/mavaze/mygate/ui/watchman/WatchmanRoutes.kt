package com.mavaze.mygate.ui.watchman

object WatchmanRoutes {
    const val HOME = "watchman/home"
    const val RESIDENT_MEMBERS = "watchman/residents"
    const val CALL_SEQUENCE = "watchman/call-sequence"
    const val VISITORS = "watchman/visitors"
    const val ADD_VISITOR = "watchman/visitors/add"
    const val VISITOR_DETAILS = "watchman/visitors/details/{visitorId}"
    const val UPDATE_VISITOR_PHOTO = "watchman/visitors/details/{visitorId}/photo"
    const val TASKS = "watchman/tasks"
    const val ADD_TASK = "watchman/tasks/add"
    const val HISTORY = "watchman/history"
    const val CALL_HISTORY = "watchman/history/calls"
    const val VISITOR_HISTORY = "watchman/history/visitors"
    const val NOTIFICATIONS = "watchman/notifications"
    const val PROFILE = "watchman/profile"

    fun visitorDetails(visitorId: String) = "watchman/visitors/details/$visitorId"
    fun updateVisitorPhoto(visitorId: String) = "watchman/visitors/details/$visitorId/photo"
}
