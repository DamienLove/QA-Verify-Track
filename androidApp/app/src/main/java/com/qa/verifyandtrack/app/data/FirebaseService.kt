package com.qa.verifyandtrack.app.data

import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.Firebase
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import com.qa.verifyandtrack.app.data.model.AppConfig
import com.qa.verifyandtrack.app.data.model.GlobalSettings
import com.qa.verifyandtrack.app.data.model.Repo
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseService(
    private val auth: FirebaseAuth = Firebase.auth,
    private val db: FirebaseFirestore = Firebase.firestore
) {

    fun authState(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth -> trySend(auth.currentUser).isSuccess }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    fun observeRepos(userId: String): Flow<List<Repo>> = callbackFlow {
        val registration: ListenerRegistration = db.collection("user_settings")
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList()).isSuccess
                    return@addSnapshotListener
                }
                val repos = snapshot?.get("repos")
                trySend(repos.toRepoList()).isSuccess
            }

        awaitClose { registration.remove() }
    }

    fun observeGlobalSettings(userId: String): Flow<GlobalSettings> = callbackFlow {
        val registration: ListenerRegistration = db.collection("user_settings")
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(GlobalSettings()).isSuccess
                    return@addSnapshotListener
                }
                val settings = snapshot?.get("globalSettings")
                trySend(settings.toGlobalSettings()).isSuccess
            }

        awaitClose { registration.remove() }
    }

    suspend fun saveRepos(userId: String, repos: List<Repo>) {
        val payload = mapOf("repos" to repos.map { repo ->
            mapOf(
                "id" to repo.id,
                "owner" to repo.owner,
                "name" to repo.name,
                "displayName" to repo.displayName,
                "apiEndpoint" to repo.apiEndpoint,
                "githubToken" to repo.githubToken,
                "useCustomToken" to repo.useCustomToken,
                "avatarUrl" to repo.avatarUrl,
                "apps" to repo.apps.map { app ->
                    mapOf(
                        "id" to app.id,
                        "name" to app.name,
                        "platform" to app.platform,
                        "playStoreUrl" to app.playStoreUrl,
                        "buildNumber" to app.buildNumber
                    )
                },
                "isConnected" to repo.isConnected,
                "projects" to repo.projects,
                "templates" to repo.templates,
                "branch" to repo.branch
            )
        })
        db.collection("user_settings")
            .document(userId)
            .set(payload, SetOptions.merge())
            .await()
    }

    suspend fun saveGlobalSettings(userId: String, settings: GlobalSettings) {
        val payload = mapOf("globalSettings" to mapOf(
            "globalGithubToken" to settings.globalGithubToken,
            "defaultBranch" to settings.defaultBranch,
            "theme" to settings.theme
        ))
        db.collection("user_settings")
            .document(userId)
            .set(payload, SetOptions.merge())
            .await()
    }

    suspend fun signIn(email: String, password: String): FirebaseUser =
        auth.signInWithEmailAndPassword(email, password).await().user
            ?: error("Missing user after sign in")

    suspend fun register(email: String, password: String): FirebaseUser =
        auth.createUserWithEmailAndPassword(email, password).await().user
            ?: error("Missing user after registration")

    suspend fun signInWithGoogle(account: GoogleSignInAccount): FirebaseUser {
        val credential: AuthCredential = GoogleAuthProvider.getCredential(account.idToken, null)
        return auth.signInWithCredential(credential).await().user
            ?: error("Missing user after Google sign in")
    }

    suspend fun signOut() {
        auth.signOut()
    }
}

private fun Any?.toRepoList(): List<Repo> {
    val rawList = this as? List<*> ?: return emptyList()
    return rawList.mapNotNull { entry ->
        val map = entry as? Map<*, *> ?: return@mapNotNull null
        Repo(
            id = map["id"] as? String ?: "",
            owner = map["owner"] as? String ?: "",
            name = map["name"] as? String ?: "",
            displayName = map["displayName"] as? String,
            apiEndpoint = map["apiEndpoint"] as? String,
            githubToken = map["githubToken"] as? String,
            useCustomToken = map["useCustomToken"] as? Boolean ?: false,
            avatarUrl = map["avatarUrl"] as? String,
            apps = (map["apps"] as? List<*>)?.mapNotNull { app ->
                val a = app as? Map<*, *> ?: return@mapNotNull null
                AppConfig(
                    id = a["id"] as? String ?: "",
                    name = a["name"] as? String ?: "",
                    platform = a["platform"] as? String ?: "android",
                    playStoreUrl = a["playStoreUrl"] as? String,
                    buildNumber = (a["buildNumber"] as? String) ?: "1"
                )
            } ?: emptyList(),
            isConnected = map["isConnected"] as? Boolean ?: true,
            projects = (map["projects"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
            templates = (map["templates"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
            branch = map["branch"] as? String ?: "main"
        )
    }
}

private fun Any?.toGlobalSettings(): GlobalSettings {
    val map = this as? Map<*, *> ?: return GlobalSettings()
    return GlobalSettings(
        globalGithubToken = map["globalGithubToken"] as? String,
        defaultBranch = map["defaultBranch"] as? String ?: "main",
        theme = map["theme"] as? String ?: "light"
    )
}
