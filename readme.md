# 计算机网络 Project1
组员：王海伟 许同樵

###  gui实现

在考虑gui的设计的时候，起初为了保证后期可能的需求和功能扩展，考虑了可扩展性，需要类似于菜单选择界面的功能，所以选择了android studio的navigation drawer activity作为模版，它提供了navigation作为菜单选择切换界面，但是提供的模版当中涉及到了Fragment和ViewModule，涉及到安卓开发的知识，出于项目重点的考虑，我们仅使用模版作为界面，不会深入的使用相关特性，仅做修改需要的学习。

server和client都有两个界面home和about，about记录作者信息和版本信息，home界面是主要的功能界面，显示输入框体，按钮以及client/server的信息，在开发过程中遇到过模拟器中的界面显示和设计不同的情况，所以后来选用了约束布局ConstraintLayout，极大程度减少布局层级（虽然对于本项目而言这不存在问题），可以很好的对所有组件进行相对布局，虽然缺点是需要考虑每个组件的id，但是对于这次的项目，组件数不多，考虑每个id的工作量不大，甚至对于绑定后端功能而言更加清晰。

项目重点应该是在ftp的实现，考虑到对安卓开发的不熟悉，和功能开发过程中可能遇到的未知问题，在界面开发上只花了较短的时间。

后续还遇到了网络权限和文件权限的问题，需要在AndroidManifest.xml中添加权限。



### server端设计思路

server除开功能以外在实现的时候有两个地方需要考虑，分别是线程管理和指令识别。

线程管理指的是当client接入之后需要合理的管控线程，既要防止资源占用过大，又要防止线程运行出现问题，同时还要考虑多链接下如何区分不同线程的信息方便调试和检查。为了方便管理和防止资源占用过大，选择线程池来管理线程，然后为了屏蔽具体实现的时候可能要考虑的线程问题，选择把线程管理和具体的功能部分隔离开来，分为两个类，负责线程管理的仅开启control的socket，当accept之后会开启新线程来调用负责功能实现的类，这样子在具体实现的时候只需要考虑单线程的情况。

对于指令识别的问题，由于使用的是kotlin，可以有类似于函数指针的特性，只需要解析出收到的指令的command和参数部分，就可以很方便的通过如下代码得到对应的实现函数：

```kotlin
private fun dispatch(cmd: String): (String) -> Pair<Int, String>? {
        return when (cmd.uppercase()) {
            "USER" -> this::user
            "PASS" -> this::pass
            "TYPE" -> this::type
            "RETR" -> this::retr
            "STOR" -> this::stor
            "NOOP" -> this::noop
            "STRU" -> this::stru
            "PASV" -> this::pasv
            "MODE" -> this::mode
            "PORT" -> this::port
            "QUIT" -> this::quit

            else -> this::noCommand
        }
    }
```

然后只需要调用返回的函数即可（这里的代码只是示意，和真实实现不同）。

```kotlin
val (cmdName, args) = parse(rawRequest)
val command = dispatch(cmdName)
val response = command(args)
```

然后就是考虑一些常量，全局声明一些端口号，新建用户的数据类然后创建用户的set用来鉴权等等不做过多说明，因为项目比较小，不考虑实现序列化然后通过文件存储这些设置，直接硬编码成了全局变量。

为了方便log的清楚记录（尤其是server比较难调试），还模仿了rfc文档里边的演示风格封装了用来log的函数，后台输出方便查看问题，后来为了方便直接输出到文件，在ftp功能实现类的参数当中加入了PrintStream类的参数，用来制定log的输出。可以是system.out，也可以是文件，甚至可以是socket。

考虑具体的功能实现，文件传输的部分会放到文件传输策略当中。

首先是鉴权，在登录的时候核验账户身份，首先配对用户名，如果是匿名用户直接完成登陆，如果是正确的名字则发送消息和reply code让client发送密码，然后验证。

MODE和STRU仅做返回消息处理，对实际程序逻辑没有影响。

TYPE的参数会作为传输文件是的transferType，使用枚举类去和用户传过来的code配对（MODE和 STRU的参数配对也是这样子的），会设置类当中的transferType，文件传输的时候会根据这个type来选择传输方式。

服务器端收到PASV之后会尝试打开端口，如果成功打开，会把相关信息返回给client，然后accept()进入阻塞等待连接，然后完成data 连接。

服务器收到port指令的时候会解析出ip地址和端口，然后建立socket连接，成功将返回成功信息，失败则根据错误类型返回（例如地址格式错误，连接建立错误）

### client端设计思路

client端主要对应server端的各种功能进行封装实现。除与server端重合的log功能、建立连接等功能外，主要需要对各类指令情况进行处理。

首先，client端需要对待发送的指令进行处理，这一功能在dispatchSend函数中实现。具体地，在PORT指令中，仅当本地成功开启端口，才会向服务端发送PORT指令；而在STOR指令中，也仅当本地成功找到指定文件，才会向服务端申请STOR指令。指令在本地都已处理成功后，dispatchSend会返回true，此时才进行与服务端在Socket上的通信，反之则直接返回错误信息。

其次，client端也需对server发回的信息进行监听。本次project中需要监听并执行的指令为client端发送PORT指令后服务端返回的内容。因此client也拥有类似于server的监听函数。

最后，client端需处理需先发送指令再处理的指令，即RETR指令。在此指令的处理中，需未等成功传送文件就先向服务器发送请求（因为需要发送请求让服务器接收文件）。

### 调试方案
最开始server是单独的一个程序（独立于安卓项目），为了方便调试，直接使用python的socket去发送消息来看log。

```python
import socket
HOST='127.0.0.1'
PORT=21
s= socket.socket()
s.connect((HOST,PORT))
#cmd=input("Please input cmd:")
s.sendall("USER test\n")
s.sendall("PASS test\n")
data=s.recv(1024)
print(data)
s.sendall("quit\n")
s.close()
#测试登陆
```

```
[05:44:31 25/11/21] <--connect1: 200 Hello world!
[05:44:31 25/11/21] connect1: USER test-->
[05:44:31 25/11/21] <--connect1: 331 password needed
[05:44:31 25/11/21] connect1: PASS test-->
[05:44:31 25/11/21] <--connect1: 230 Logged in as test
[05:44:31 25/11/21] connect1: quit-->
[05:44:31 25/11/21] <--connect1: 221 See ya.
```

```python
import socket

HOST='127.0.0.1'
PORT=21
a= socket.socket()
a.connect((HOST,PORT))


host = '127.0.0.1'
port = 9999
s= socket.socket()
s.bind((host, port))
s.listen(1)
a.sendall("PORT 127,0,0,1,39,54\n")

clientsocket, addr = s.accept()

print(clientsocket, addr)

a.sendall("QUIT\n")
s.close()
a.close()
#测试port
```

```
[09:04:03 25/11/21] <--connect1: 200 Hello world!
[09:04:03 25/11/21] connect1: PORT 127,0,0,1,39,54-->
[09:04:03 25/11/21] <--connect1: 200 Connected to /127.0.0.1:9999
[09:04:03 25/11/21] connect1: QUIT-->
[09:04:03 25/11/21] <--connect1: 221 See ya.
```

但是后期移植安卓平台上的时候由于设备问题（macbook），无法方便的连接手机，而且在手机上运行程序无法很好的调试，所以还是选择下功夫使用模拟器调试，需要一些步骤，首先模拟器的target需要选择google apis版本的系统，然后需要配置好adb调试，通过adb shell连接到qemu模拟器的shell，去修改data文件夹的全选，方便做文件的操作（把测试文件放到模拟器中），之后就可以打开client和server的安卓项目同时开启debug来调试了，比起在两台手机上方便许多。

### 文件传输策略

我们的策略其实是比较简陋的，所以没有在视屏当中演示，对于小文件而言没有明显的差别，对于大文件（500mb）而言如果使用旧的传输方案可能需要数分钟。

主要的传输函数如下，当中包含了一些md5的计算，简单来说就是在两个流之间建立一个buffer，进行流和流之间的数据传输，没有特殊的地方。

```kotlin
fun transferTo(input: InputStream, out: OutputStream): Long {
        Objects.requireNonNull(out, "out")
        messageDigest.reset()
        var transferred = 0L
        var read: Int
        val buffer = ByteArray(8192)
        while (input.read(buffer, 0, 8192).also { read = it } >= 0) {
            out.write(buffer, 0, read)
            messageDigest.update(buffer, 0, read);
            transferred += read.toLong()
        }
        return transferred
    }
```

旧的传输方案之所以会达到几分钟，主要原因在于client最初的处理方式存在问题，client一开始的逻辑是先把整个文件的数据从文件流读到socket输出流上面，然后向server发送stor指令，让server开始接收，这在小文件的时候是完全可行的，但是500m的文件上就会出现问题，500m的数据在socket的输出上的时候会卡死，优化的方案是先向server发送指令，之后在开始读取文件到socket上，同时server那边同步接收。其实这个问题我个人觉得是个bug，不太能够称之为优化,修复过后500mb的文件大约8到10秒，其中还包含了计算md5的时间
