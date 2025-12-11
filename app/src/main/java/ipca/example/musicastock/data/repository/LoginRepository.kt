package ipca.example.musicastock.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import ipca.example.musicastock.data.ResultWrapper
import ipca.example.musicastock.domain.repository.ILoginRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class LoginRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore
) : ILoginRepository {

    override fun login(email: String, password: String): Flow<ResultWrapper<Unit>> = flow {
        try {
            emit(ResultWrapper.Loading())

            val result = auth.signInWithEmailAndPassword(email, password).await()

            result.user?.email?.let {
                db.collection("users")
                    .document(result.user!!.uid)
                    .set(mapOf("email" to it))
                    .await()
            }

            emit(ResultWrapper.Success(Unit))

        } catch (e: Exception) {
            emit(ResultWrapper.Error(e.message ?: "Erro desconhecido"))
        }

    }.flowOn(Dispatchers.IO)


    override fun register(email: String, password: String): Flow<ResultWrapper<Unit>> = flow {
        try {
            emit(ResultWrapper.Loading())

            val result = auth.createUserWithEmailAndPassword(email, password).await()

            db.collection("users")
                .document(result.user!!.uid)
                .set(mapOf("email" to email))
                .await()

            emit(ResultWrapper.Success(Unit))

        } catch (e: Exception) {
            emit(ResultWrapper.Error(e.message ?: "Erro ao registar"))
        }

    }.flowOn(Dispatchers.IO)
}
