package com.jnkim.poschedule.ui.nav

sealed class Route(val path: String) {
    object Login : Route("login")
    object Today : Route("today")
    object Settings : Route("settings")
    object PlanEditor : Route("plan_editor")
    object TidySnap : Route("tidy_snap")
    object Debug : Route("debug")
}
