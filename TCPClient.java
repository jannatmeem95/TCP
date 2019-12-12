import java.io.*;
import java.net.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

class TCPClient
{
    public static void main(String argv[]) throws Exception
    {
        String sentence;
        String modifiedSentence;
        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
        try {

            Socket clientSocket = new Socket("localhost", 6789);
            DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
            BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            System.out.println("Enter UserId : ");
            sentence = inFromUser.readLine();
            outToServer.writeBytes(sentence + '\n');
            modifiedSentence = inFromServer.readLine();
            int n = modifiedSentence.length() - 2;
            System.out.println(modifiedSentence);
            String crash = "Logged out.";
            if (modifiedSentence.charAt(n) == 's') {
                // outToServer.writeBytes(crash);
                return;
            } else {
                System.out.print("What do you want to do?");
                String event = inFromUser.readLine();
                if (event.contains("send")) {

                    System.out.println("Enter Receiver's ID : ");
                    sentence = inFromUser.readLine();

                    outToServer.writeBytes(sentence + '\n');
                    modifiedSentence = inFromServer.readLine();
                    //System.out.println(modifiedSentence);
                    if (modifiedSentence.charAt(0) != 'R') {


                        System.out.println("Enter Filename : ");
                        String filename = inFromUser.readLine();

                        File some = new File(filename);

                        File gradeList = new File(filename);
                        if (!gradeList.exists()) {
                            throw new FileNotFoundException("Failed to find file: " +
                                    gradeList.getAbsolutePath());
                        }


                        Path path = Paths.get(filename);
                        byte[] data = Files.readAllBytes(path);
                        int sze = (int) data.length;

                        System.out.println("bytearray "+  sze);

                        outToServer.writeBytes(filename + " " + sze + "\n");
                        String esentence = inFromServer.readLine();

                        System.out.println(esentence);

                        if (esentence.charAt(0) == 'N') {
                            System.out.println(esentence);
                            outToServer.writeBytes(crash);
                            return;
                        } else {  //file transmission
                            sentence = esentence.substring(esentence.indexOf('S'), esentence.indexOf('f') + 1);
                            System.out.println(sentence);
                            int fileID = Integer.parseInt(esentence.substring(0, esentence.indexOf('S')));
                            int chunk = Integer.parseInt(esentence.substring(esentence.indexOf('f') + 1));
                            System.out.println(fileID + "  " + chunk);

                            int total_chunk = 0;

                            if (sze % chunk == 0) total_chunk = (int) sze / chunk;
                            else {
                                total_chunk = (int) sze / chunk + 1;
                            }

                            System.out.println("total_chunk " + total_chunk );
                            outToServer.writeBytes(total_chunk+"\n");
                            outToServer.flush();
                            int temp_size = 0;
                            byte[] each_chunk = new byte[chunk];
                            int file_index = 0;

                            while (temp_size != total_chunk)  //sending chunks
                            {
                                //int chunk_size=chunk;
                                //each_chunk[0] = (byte) chunk;
                                for (int i = 0; i < chunk; i++) {
                                    if (file_index >= data.length) {
                                        each_chunk[i] = 0;
                                    } else {

                                        each_chunk[i] = data[file_index];
                                    }
                                    file_index++;
                                    System.out.print(" okk "+i);
                                }

                                outToServer.write(each_chunk, 0, chunk);
                                outToServer.flush();


                                temp_size++;

                                long start_time = System.currentTimeMillis();
                                String server_acknowledgement = inFromServer.readLine();
                                long end_time = System.currentTimeMillis();
                                System.out.println(server_acknowledgement);

                                if (end_time - start_time > 30 * 1000) {
                                    outToServer.writeBytes("Timeout: Cancel transmission." + "\n");
                                    break;
                                } else outToServer.writeBytes("Next chunk" + "\n");
                                outToServer.flush();

                            }


                            outToServer.writeBytes("Send to receiver" + "\n");
                            System.out.println(inFromServer.readLine());
                            return;
                        }
                    } else {           //receiver offline
                        System.out.println(modifiedSentence);
                        outToServer.writeBytes(crash+"\n");
                        return;
                    }


                } else {
                    String receive = inFromServer.readLine();
                    System.out.println(receive);
                    System.out.println("Enter 1 to accept :");
                    String s=inFromUser.readLine();
                    System.out.println(s);
                    outToServer.flush();
                    int p=Integer.parseInt(s);
                    if(s.equals("1")) {
                        outToServer.writeBytes(s + "\n");
                    }else{
                        outToServer.writeBytes(s +"no"+ "\n");
                    }
                    System.out.println(inFromServer.readLine());
                    return;
                }

            }
        }
        catch (Exception e){
            System.out.println("Server not found");return;
        }
    }
}