plugins {
    id("kmp.library")
}

// Charter: business policy and nothing else.
//
// commonMain has no dependencies at all. Not coroutines, because `suspend` is a
// language feature rather than a library one. Not Koin, because assembling the
// object graph is the composition root's job, not the domain's. Adding anything
// to this block should require an argument.
kotlin {
    sourceSets {
        commonMain.dependencies {
            // The one permitted dependency, added when `Flow` first appeared in a
            // repository signature (TodoRepository). `api` because it is part of that
            // public signature. ADR-0004 anticipated exactly this.
            api(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
