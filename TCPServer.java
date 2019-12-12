import java.io.*;
import java.net.*;
import java.util.Hashtable;
import java.util.Objects;
import java.util.Random;


public class TCPServer
{
    public static void main(String argv[]) throws Exception
    {
        int workerThreadCount = 0;
        int id = 1;
        ManageHash A=new ManageHash();
        SizeManager S=new SizeManager();
        TransmissionManager T=new TransmissionManager();
        ServerSocket welcomeSocket = new ServerSocket(6789);
        while(true)
        {
            Socket connectionSocket = welcomeSocket.accept();

            WorkerThread wt = new WorkerThread(connectionSocket,A,S,T);
            Thread t = new Thread(wt);
            t.start();
            workerThreadCount++;
            id++;

        }

    }
}

class SizeManager
{
    int size;
    int fileID=0;
    SizeManager(){size= 30*1016*1016;System.out.println("Initial Size in server: "+size);}
    boolean available(float n)
    {
        if(n<=size) return true;
        return false;
    }
    int decrease_size(int n)
    {
        if(available(n)){
            size-=n;
            fileID++;
            System.out.println("available Size in server: "+size);
            return fileID;
        }
        return -1;
    }
    int increase_size(int n)
    {
        if(available(n)){
            size+=n;
            fileID--;System.out.println("available Size in server: "+size);
            return fileID;
        }
        return -1;
    }


}

class UserInfo
{
    String IPport;
    Socket consocket;
    UserInfo(String a,Socket b){ IPport=a; consocket=b; }
}

class ManageHash
{
    public Hashtable<Integer,UserInfo>bucket=new Hashtable<>();
    synchronized boolean search(Integer x)
    {
        if(bucket.containsKey(x)) return true;
        return false;
    }
    synchronized String insert(Integer x,UserInfo y)
    {
        if(search(x)==false || (search(x)==true && Objects.equals(bucket.get(x).IPport, y.IPport)) )
        {
            bucket.put(x,y);
            System.out.println(x+" "+y.IPport);
            return "ID "+x+" logged in.";
        }
        else return "ID " + x + " is already logged in from another IP address.";
    }

    synchronized void delete(Integer x)
    {
        if(search(x)) {
            bucket.remove(x);
        }
    }


}

class FileDetails
{
    int sender;
    int receiver;
    int max_chunk;
    int chunk_no;
    byte[][] data;
    int id;
    int filesize;
    int cursize;
    int temp=0;
    FileDetails(int x,int y,int z,int u,int f)
    {
        sender=x;
        receiver=y;
        max_chunk=z;
        id=0;
        chunk_no=u;
        filesize=f;
        data=new byte[chunk_no][max_chunk];
        cursize=0;
    }
    String insert_chunk(byte[] chunk)
    {
        int len=max_chunk;
        //System.out.print("chunk_index : ");
        for(int i=0;i<max_chunk;i++)
        {
            if(temp>=filesize){
                len--;
                // System.out.println("last chunk detected "+len);
                data[id][i]=0;
            }
            else {
                data[id][i] = chunk[i];
            }

            //System.out.print(" temp "+temp+" i "+i + " ");

            temp++;
            //System.out.print("temp "+temp+" i "+i + " ");

        }
        // System.out.println("chunk id :"+id+" chunk array length : "+data[id].length);
        //System.out.println("yaa "+new String(data[id]));
        String x="chunk "+id+"transmitted successfully";
        id++;
        cursize+=len;
        System.out.println("current total size of all chunks : "+cursize);

        return x;
    }

    void delete_details()
    {
        int i=0,j=0;
        while ((i!=id)){
            while(j!=max_chunk||data[i][j]!=0)
            {
                data[i][j]=0;
                j++;
            }
            j=0;
            i++;
        }

    }

    boolean check_chunks()
    {
        if(cursize==filesize) return true;
        return false;
    }
}

class TransmissionManager
{
    public Hashtable<Integer,FileDetails>FileManager=new Hashtable<>(); //key fileid;

    synchronized boolean search(Integer x)
    {
        if(FileManager.containsKey(x)) return true;
        return false;
    }
    synchronized String insert(Integer x,FileDetails y)
    {
        if(!FileManager.containsKey(x)){
            FileManager.put(x,y);
            return "filedetails of "+x+" inserted in hashtable.";
        }
        return "filedetails of "+x+" could not be inserted in hashtable.";

    }
    synchronized int delete(Integer x)
    {
        if(search(x)){
            int f=FileManager.get(x).sender;
            FileManager.remove(x);
            return f;
        }
        return -1;

    }
}

class WorkerThread implements Runnable
{
    private Socket connectionSocket;
    private int id;
    ManageHash map;
    SizeManager sz;
    TransmissionManager tr;
    int fId=-1;
    int CliendId=-1;
    int mode=0;

    public WorkerThread(Socket ConnectionSocket,ManageHash M,SizeManager S,TransmissionManager T)
    {
        this.connectionSocket=ConnectionSocket;
        this.map=M;
        this.sz=S;
        this.tr=T;

    }

    public void check(String s,int x)
    {
        if(s.substring(0,2)=="Log") map.delete(x);
    }

    public void run()
    {
        String clientSentence;
        int chunk=0;

        while(true)
        {
            try
            {
                DataOutputStream outToServer = new DataOutputStream(connectionSocket.getOutputStream());
                InputStreamReader x=new InputStreamReader(connectionSocket.getInputStream());
                BufferedReader inFromServer = new BufferedReader(x);

                clientSentence = inFromServer.readLine();
                int userId=Integer.parseInt(clientSentence);
                // System.out.println(map.check());
                UserInfo user=new UserInfo(connectionSocket.getInetAddress()+" "
                        +connectionSocket.getPort(),connectionSocket);
                String out=map.insert(userId,user);
                outToServer.writeBytes(out+"\n");
                //System.out.println(out);
                CliendId=userId;

                clientSentence=inFromServer.readLine();
                int receiverId=Integer.parseInt(clientSentence);



                if(map.search(receiverId)==false){
                    out="Receiver is offline.File transmission request denied." ;
                    mode=2;
                    map.delete(userId);
                    outToServer.writeBytes(out + "\n");
                    System.out.println(out);
                }
                else {
                    out = "found";

                    outToServer.writeBytes(out + "\n");
                    String p = inFromServer.readLine();
                    check(p, userId);


                    System.out.println(p);
                    int r = p.indexOf(' ');
                    String filename = p.substring(0, r);
                    //System.out.println(filename);
                    String filesize = p.substring(r + 1);
                    int fsize = Integer.parseInt(filesize);


                    //  System.out.println(fsize);
                    int zz=sz.decrease_size(fsize);

                    if (zz == -1) {
                        out = "Not enough memory.";
                        map.delete(userId);
                        int d=sz.increase_size(fsize);
                        System.out.println(out);
                        outToServer.writeBytes(out + "\n");
                    } else {    //file tranmission
                        Random rand = new Random();
                        chunk=rand.nextInt(1024);
                        //chunk = 4;
                        out = sz.fileID + "Start transmission with each chunk of" + chunk;
                        //System.out.println(out);

                        fId=sz.fileID;
                        outToServer.writeBytes(out + "\n");

                        String tot_chunk=inFromServer.readLine();
                        int total_chunk = Integer.parseInt(tot_chunk);
                        System.out.println("total_chunk " + total_chunk);




                        int temp_size = 0;
                        byte[] each_chunk = new byte[chunk + 1];

                        FileDetails f = new FileDetails(userId, receiverId, chunk, total_chunk, fsize);
                        boolean transmit = true;
                        mode=3;

                        while (temp_size != total_chunk) {//filedetails e ekta kore chunk insert kortesi

                            int value=connectionSocket.getInputStream().read(each_chunk);
                            System.out.println("value "+value);


                            String ack = f.insert_chunk(each_chunk);
                            System.out.println(ack);
                            outToServer.writeBytes(ack + "\n");


                            temp_size++;

                            String client_acknowledgement = inFromServer.readLine();
                            System.out.println(client_acknowledgement);
                            if (client_acknowledgement.contains("Cancel")) {
                                int d=sz.increase_size(fsize);
                                mode=6;
                                transmit = false;
                                break;
                            }

                        }

                        mode=10;
                        System.out.println(inFromServer.readLine());
                        if (f.check_chunks()) {
                            outToServer.writeBytes("File transmission successful" + "\n");
                        } else {
                            outToServer.writeBytes("Error transmitting files" + "\n");
                            transmit = false;
                            int d=sz.increase_size(fsize);

                        }

                        if (transmit) {
                            System.out.println(tr.insert(sz.fileID, f));
                        }



                        mode=8;
                        FileDetails rk;
                        if (tr.search(sz.fileID)) {
                            rk = tr.FileManager.get(sz.fileID);
                            System.out.println(tr.search(sz.fileID));
                            UserInfo c = map.bucket.get(rk.receiver);
                            System.out.println("Receiverport " + c.IPport + "\n");


                            DataOutputStream outToServer2 = new DataOutputStream(c.consocket.getOutputStream());
                            InputStream rec=c.consocket.getInputStream();

                            outToServer2.writeBytes("File transmission request from "+CliendId+"\n");
                            outToServer2.flush();
                            int dd;
                            int ok=rec.read();

                            System.out.println(" ok "+ok);
                            FileDetails ff=tr.FileManager.get(fId);

                            if(ok==10){

                                String file_ext=filename.substring(filename.length()-3,filename.length());
                                FileOutputStream fos =
                                        new FileOutputStream("E:\\TCPClient\\src\\"+CliendId+"_to_"+receiverId+"."+file_ext);
                                byte [] myByteArray=new byte[ff.filesize];
                                int index=0;int tem=0;
                                for(int k=0;k<total_chunk;k++){

                                    for(int l=0;l<chunk;l++){
                                        if(index==ff.filesize){ tem=1; break;}
                                        myByteArray[index]=ff.data[k][l];

                                        index++;
                                    }

                                    if (tem==1) break;;
                                }
                                // System.out.println(new String(myByteArray));
                                fos.write(myByteArray);
                                fos.close();
                                outToServer2.writeBytes("File received."+"\n");
                            }

                            else{
                                System.out.println("Request denied");
                                outToServer2.writeBytes("Request denied"+"\n");
                            }

                            tr.FileManager.remove(fId);

                        }

                    }
                }

            }
            catch(Exception e)
            {

                if(tr.search(fId)&&mode==6){
                    FileDetails f=tr.FileManager.get(fId);
                    tr.FileManager.remove(fId);
                    System.out.println(f.sender+" "+"logged out");
                    map.delete(f.sender);return;

                }
                else if(mode==2) {
                    System.out.println("Receiver offline");return;
                }
                else
                {
                    System.out.println(CliendId+" "+"logged out");
                    if(map.search(CliendId)) map.delete(CliendId);
                    return;
                }

            }
        }
    }
}