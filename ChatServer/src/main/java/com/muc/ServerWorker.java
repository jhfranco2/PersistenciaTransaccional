package com.muc;


import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.net.Socket;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

/**
 * Created by jim on 4/18/17.
 */
public class ServerWorker extends Thread {

    private final Socket clientSocket;
    private final Server server;
    private String login = null;
    private OutputStream outputStream;

    private HashSet<String> topicSet = new HashSet<>();
    public ServerWorker(Server server, Socket clientSocket) {
        this.server = server;
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        try {
            handleClientSocket();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void handleClientSocket() throws IOException, InterruptedException {
        InputStream inputStream = clientSocket.getInputStream();
        this.outputStream = clientSocket.getOutputStream();

        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        //Este bucle lee lineas del cliente y luego estamos entregando diferentes lineas de codigo dependiendo de lo que se quiere hacer.
        while ( (line = reader.readLine()) != null) {
            String[] tokens = StringUtils.split(line);
            if (tokens != null && tokens.length > 0) {
                String cmd = tokens[0];
                if ("logoff".equals(cmd) || "quit".equalsIgnoreCase(cmd)) {
                    handleLogoff();
                    break;
                } else if ("login".equalsIgnoreCase(cmd)) {
                    handleLogin(outputStream, tokens);
                } else if("msg".equalsIgnoreCase(cmd)){
                    String[] tokensMsg= StringUtils.split(line,null, 3);
                    handleMessage(tokensMsg);
                } else if ("join".equalsIgnoreCase(cmd)) {
                    handleJoin(tokens);
                } else if ("leave".equalsIgnoreCase(cmd)) {
                  heandleLeave(tokens);
                } else {
                    String msg = "desconocido " + cmd + "\n";
                    outputStream.write(msg.getBytes());
                }
            }
        }

        clientSocket.close();
    }

    private void heandleLeave(String[] tokens) {
        if(tokens.length >1){
            String topic = tokens[1];
            topicSet.remove(topic);


        }
    }

    //Mira si eres miembro de el grupo
    public boolean isMenberOfTopic(String topic){
        return topicSet.contains(topic);
    }
    //Para hacer grupos

    private void handleJoin(String[] tokens) {
        if(tokens.length >1){
            String topic = tokens[1];
            topicSet.add(topic);


        }
    }

    //formato : "msg" "login" body...
    //formato: "msg" "topic" body
    //Para enviarmensajes
    private void handleMessage(String[] tokens) throws IOException {
        String sendTo = tokens[1];
        String body = tokens[2];

        boolean isTopic = sendTo.charAt(0)== '#';

        List<ServerWorker> workerList = server.getWorkerList();
        for(ServerWorker worker : workerList) {
            if (isTopic) {
                if(worker.isMenberOfTopic(sendTo)){

                    String outMsg = "msg " + sendTo+":"+login + " " + body + "\n";
                    worker.send(outMsg);
                }
            } else {
                if (sendTo.equalsIgnoreCase(worker.getLogin())) {
                    String outMsg = "msg " + login + " " + body + "\n";
                    worker.send(outMsg);
                }
            }
        }

    }

    //Sirve para cuando quieras deslogueart
    private void handleLogoff() throws IOException {
        server.removeWorker(this);
        List<ServerWorker> workerList = server.getWorkerList();

        // send other online users current user's status
        String onlineMsg = "offline " + login + "\n";
        for(ServerWorker worker : workerList) {
            if (!login.equals(worker.getLogin())) {
                worker.send(onlineMsg);
            }
        }
        clientSocket.close();
    }

    public String getLogin() {
        return login;
    }

    //Metodo para iniciar sesion
    private void handleLogin(OutputStream outputStream, String[] tokens) throws IOException {
        if (tokens.length == 3) {
            String login = tokens[1];
            String password = tokens[2];

            if ((login.equals("guest") && password.equals("guest")) || (login.equals("jim") && password.equals("jim")) ) {
                String msg = "inicio correcto\n";
                outputStream.write(msg.getBytes());
                this.login = login;
                System.out.println("Usuario logueado satisfactoriamente: " + login);

                List<ServerWorker> workerList = server.getWorkerList();

                // send current user all other online logins
                for(ServerWorker worker : workerList) {
                    if (worker.getLogin() != null) {
                        if (!login.equals(worker.getLogin())) {
                            String msg2 = "En linea" + worker.getLogin() + "\n";
                            send(msg2);
                        }
                    }
                }

                // envia el mensaje por si otro usuario esta en linea
                String onlineMsg = "En linea " + login + "\n";
                for(ServerWorker worker : workerList) {
                    if (!login.equals(worker.getLogin())) {
                        worker.send(onlineMsg);
                    }
                }
            } else {
                String msg = "Error al logearse\n";
                outputStream.write(msg.getBytes());
            }
        }
    }

    private void send(String msg) throws IOException {
        if (login != null) {
            outputStream.write(msg.getBytes());
        }
    }
}