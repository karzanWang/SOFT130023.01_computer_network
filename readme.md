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

为了方便调试，直接使用python的socket去发送消息来看log

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

剩下的只需要考虑具体的功能实现了。文件传输的部分会放到文件传输策略当中。

首先是鉴权，在登录的时候核验账户身份，首先配对用户名，如果是匿名用户直接完成登陆，如果是正确的名字则发送消息和reply code让client发送密码，然后验证。

MODE和STRU仅做返回消息处理，对实际程序逻辑没有影响。

TYPE的参数会作为传输文件是的transferType，使用枚举类去和用户传过来的code配对（MODE和 STRU的参数配对也是这样子的），会设置类当中的transferType，文件传输的时候会根据这个type来选择传输方式。

PASV模式是比较复杂的，服务器端的逻辑是收到PASV

### 文件传输策略

