package pro.eugw.lcapi

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

class RequestQueue : Thread() {

    private var queue = ArrayList<ReqLis>()
    private var newQueue = ArrayList<ReqLis>()
    private val client = OkHttpClient()
    private var firstStart = true

    fun addRequest(request: Request, listener: Listener) {
        newQueue.add(ReqLis(request, listener))
        if (firstStart) {
            firstStart = false
            start()
        }
    }

    override fun run() {
        super.run()
        while (true) {
            if (queue.isNotEmpty())
                queue.forEach {
                    val res = client.newCall(it.request).execute()
                    if (res.isSuccessful)
                        it.listener.onSuccess(res)
                    else
                        it.listener.onFail(res)
                    res.close()
                    sleep(3000)
                }
            queue.clear()
            queue.addAll(newQueue)
            newQueue.clear()
        }
    }

    interface Listener {
        fun onSuccess(response: Response)
        fun onFail(response: Response)
    }

    class ReqLis(val request: Request, val listener: Listener)

}