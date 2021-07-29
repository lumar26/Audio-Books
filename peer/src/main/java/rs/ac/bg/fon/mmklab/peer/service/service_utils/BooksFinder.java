package rs.ac.bg.fon.mmklab.peer.service.service_utils;

import rs.ac.bg.fon.mmklab.book.AudioBook;
import rs.ac.bg.fon.mmklab.book.AudioDescription;
import rs.ac.bg.fon.mmklab.book.BookInfo;
import rs.ac.bg.fon.mmklab.book.BookOwner;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Collectors;

import static java.nio.file.FileVisitOption.FOLLOW_LINKS;

public class BooksFinder extends SimpleFileVisitor<Path> {
    /* Ova klasa sadrzi:
     * 1. metodu koja na osnovu prosledjene putanje ka folderu vraca listu audio knjiga koje u njoj postoje
     *
     * 2. pomocnu metodu koja za svaki audio fajl kreira jedan objekat tipa AudioBook koja sadrzi osnovne informacije o knjizi
     */


    public static List<AudioBook> fetchBooks(String booksFolder, String audioFormatExtension) {
        Path pathToBooksFolder = Paths.get(booksFolder);
//        provera da li je prosledjena putanja postojeca
        if (Files.notExists(pathToBooksFolder)){
//            ovde isto da se napravi neki exceptoin
            System.err.println("Greska (fetchBooks): Prosledjena putanja ka folderu sa knjigama je nepostojeca");
            return null;
        }

        try {
//            trebalo bi da moze ovako odmah da se strim path-ova mapira u strim fajlova pa da se to sve ubaci u listu. Files.walk vraca stream
            List<File> filesInDirectory = Files.walk(pathToBooksFolder, FOLLOW_LINKS).
                    map(path -> new File(path.toString()))
                    .collect(Collectors.toList());
            System.out.println("(fetchBooks): velicina liste: " + filesInDirectory.size());
            filesInDirectory.forEach(file -> file.toString());

//            izbacujemo sve fajlove koji nisu u zadatom formatu, pretpostavka je da ce svi fajlovi bit u istom formatu: .wav
            List<File> booksInDirectory = filesInDirectory.stream().
                    filter(file -> file.getPath().endsWith(audioFormatExtension))
                    .collect(Collectors.toList());

            if (filesInDirectory.size() == 0) {
//                nema audio knjiga u direktorijumu
                System.err.println("Greska (fetchBooks): nema ni jedne knjige u navedenom direktorijumu");
            }

            List<AudioBook> resultList = booksInDirectory.stream().
                    map(bookFile -> {
                        try {
                            return new AudioBook(getAudioDescription(bookFile), getBookInfo(bookFile, audioFormatExtension), getBookOwner());
                        } catch (Exception e) {
//                            nije bilo moguce pronaci vlasnika knjige, hendluj to
                            System.err.println("Greska (fetchBooks): nije pronadjen vlasnik knjige, nije prepoznat localhost");
//                            e.printStackTrace();
                        }
                        return null;
                    })
                    .filter(element -> element != null) // za sad sam ovde odradio da ako se desi da se ne nadje neki bookOwner za odredjeni fajl da se samo izbaci iz liste
//                    moglo bi ovde eventualno i da se doda da se taj fajl izbrise jer dolazi do neke greske
                    .collect(Collectors.toList());
            return resultList;
        } catch (IOException e) {
            System.err.println("Generalna greska (fetchBooks):  Nije prosao glavni try blok ");
//            e.printStackTrace();
        }
        return null;
    }

    private static BookInfo getBookInfo(File bookFile, String fileExtension) {
        Path filePath = Paths.get(bookFile.toString());
        String fileName = filePath.getFileName().toString();
        String bookName, bookAuthor;

        if (!fileName.endsWith(fileName)) {
//            bacamo neki izuzetak jer ekstenzija nije ta koja je navedena
        }
//        odbacujemo deo koji oznacava ekstenziju
        fileName = fileName.substring(0, fileName.length() - fileExtension.length());
        String[] infos = fileName.split("-");
        bookName = infos[1].replace('_', ' ');
        bookAuthor = infos[0].replace('_', ' ');
        return new BookInfo(bookName, bookAuthor);
    }

    private static BookOwner getBookOwner() throws Exception {
        int port = 5005; // ovde ce morati neka metoda koja nam daje port
        boolean isOnline = true;
        InetAddress bookOwnerAddress = null;
        try {
            bookOwnerAddress = InetAddress.getByName("localhost");
        } catch (UnknownHostException e) {
//            e.printStackTrace();
            System.err.println("Greska (getBookOwner): getByName(\"localhost\") nije odradio posaa");
//            ovo jos ne znam kako bi moglo da se hendluje i ukojim slucajevima moze da se desi da baci izuzetak
            isOnline = false;
        }
        if (bookOwnerAddress != null)
            return new BookOwner(bookOwnerAddress, port, isOnline);
        else
//            ovo takodje mora da se resi sa nekim custom izuzetkom
            System.err.println("Greska (getBookOwner): bookOwner == null");
            throw new Exception("ERROR: Could not find book owner");
    }

    private static AudioDescription getAudioDescription(File bookfile){
//        odradi ovde neku drugaciju inicijalizaciju mislim da ovo nije dobro ovako...
        AudioInputStream audioInputStream = null;
        AudioFormat audioFormat = null;
        long lengthInFrames = 0;
        int frameSizeInBytes = 0;
        try{
            audioInputStream = AudioSystem.getAudioInputStream(bookfile);
            audioFormat = audioInputStream.getFormat();
            lengthInFrames = audioInputStream.getFrameLength();
            frameSizeInBytes = audioFormat.getFrameSize();
        } catch (UnsupportedAudioFileException e) {
//            ako u getAudioInputStyream prosledimo neodgovarajuci format fajla
            System.err.println("Greska (getAudioDescription): prosledjen je fajl koji nije audio");
//            e.printStackTrace();
        } catch (IOException e) {
//            za getAudioInputStream
            System.err.println("Greska (getAudioDescription): getAudioInputStream ne moze da povuce strim iz datog fajla");
//            e.printStackTrace();
        }

        return new AudioDescription(audioInputStream, audioFormat, lengthInFrames, frameSizeInBytes);
    }
}
