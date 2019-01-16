package pro.eugw.lcapi

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

class RequestQueue : Thread() {

    private var queue = ArrayList<ReqLis>()
    private val client = OkHttpClient()
    private var first = true

    fun addRequest(request: Request, listener: Listener) {
        queue.add(ReqLis(request, listener))
        if (first) {
            first = false
            start()
        }
    }

    override fun run() {
        super.run()
        while (true) {
            if (queue.isNotEmpty()) {
                val ooo = queue[0]
                val res = client.newCall(ooo.request).execute()
                if (res.isSuccessful)
                    ooo.listener.onSuccess(res)
                else
                    ooo.listener.onFail(res)
                res.close()
                queue.remove(ooo)
            }
            sleep(3000)
        }

    }

    interface Listener {
        fun onSuccess(response: Response)
        fun onFail(response: Response)
    }

    class ReqLis(val request: Request, val listener: Listener)

}