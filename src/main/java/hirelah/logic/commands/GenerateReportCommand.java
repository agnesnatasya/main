package hirelah.logic.commands;

import hirelah.logic.commands.exceptions.CommandException;
import hirelah.model.Model;
import hirelah.model.hirelah.Interviewee;
import hirelah.model.hirelah.IntervieweeList;
import hirelah.model.hirelah.Remark;
import hirelah.model.hirelah.exceptions.IllegalActionException;
import hirelah.storage.Storage;
import javafx.collections.ObservableList;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.util.*;
import java.io.IOException;
import java.util.ArrayList;

import static hirelah.logic.commands.OpenReportCommand.MESSAGE_NOT_INTERVIEWED;
import static java.util.Objects.requireNonNull;


public class GenerateReportCommand extends Command {

    public static final String COMMAND_WORD = "report";
    public static final String MESSAGE_SUCCESS = "Report of %s generated.";
    public static final String MESSAGE_FORMAT = COMMAND_WORD + " <interviewee>";
    public static final String MESSAGE_FUNCTION = ": Generates the interview report of a particular interviewee in PDF.\n";
    public static final String MESSAGE_USAGE = MESSAGE_FORMAT
            + MESSAGE_FUNCTION
            + "Example: " + COMMAND_WORD
            + " Jane Doe ";

    private final String identifier;

    static class SplitPageRemarks {
        private final ArrayList<String> remainingSplitRemarks;
        private final String duration;

        SplitPageRemarks (ArrayList<String> remainingSplitRemarks, String duration) {
            this.remainingSplitRemarks = remainingSplitRemarks;
            this.duration = duration;
        }
    }

    public GenerateReportCommand(String identifier) {
        this.identifier = identifier;
    }

    @Override
    public CommandResult execute(Model model, Storage storage) throws CommandException {
        requireNonNull(model);
        IntervieweeList interviewees = model.getIntervieweeList();
        Interviewee interviewee;
        try {
            interviewee = interviewees.getInterviewee(identifier);
            if (!interviewee.isInterviewed()) {
                throw new CommandException(String.format(MESSAGE_NOT_INTERVIEWED, interviewee));
            }
            model.setCurrentInterviewee(interviewee);
            generateReport(interviewee);
        } catch (IllegalActionException e) {
            throw new CommandException(e.getMessage());
        }
        return new CommandResult(String.format(MESSAGE_SUCCESS, interviewee));
    }

    /**
     * Generate interview report in the form of PDF for a particular interviewee.
     *
     * @param interviewee the interviewee.
     */
    private void generateReport(Interviewee interviewee) throws CommandException {
        try {
            ObservableList<Remark> remarks = interviewee
                    .getTranscript()
                    .orElseThrow(() ->
                            new CommandException("The transcript for this interviewee could not be found"))
                    .getRemarkListView();


            PDDocument document = new PDDocument();
            /*PDFPage currentPage = newPage(document);

            for (Remark currentRemark:remarks) {
                currentPage.getPageContentStream().beginText();
                printTime(currentRemark.getTimeString(), currentPage);
                printRemark(currentRemark.getMessage(), currentPage);
                if (currentPage.isEndOfPage(20)) {
                    currentPage.getPageContentStream().endText();
                    currentPage.getPageContentStream().close();
                    currentPage = newPage(document);
                }
            }*/
            ArrayList<Remark> remarkList = new ArrayList<>();
            remarkList.addAll(remarks);
            while (remarkList.size() > 0) {
                printReport(document, remarkList);
            }
            //currentPage.getPageContentStream().endText();
            //currentPage.getPageContentStream().close();
            document.save("data/Jo.pdf");
            document.close();
        } catch (IOException e) {
            throw new CommandException("There is an error in generating the report!");
        }
    }

    /**
     * Generate new page.
     */
    private static PDPage printReport(PDDocument document, ArrayList<Remark> remarkList) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        PDPageContentStream pageContentStream = new PDPageContentStream(document, page);
        pageContentStream.setFont( PDType1Font.HELVETICA , 12 );
        pageContentStream.beginText();
        pageContentStream.newLineAtOffset(100, 800);
        int currentYOffset = 0;
        while (!remarkList.isEmpty()) {
            Remark currentRemark = remarkList.get(0);
            printTime(currentRemark.getTimeString(), pageContentStream);
            ArrayList<String> splitRemark = splitRemark(currentRemark.getMessage());
            while (currentYOffset + (splitRemark.size() *10) < 780 && !remarkList.isEmpty()) {
                printRemark(splitRemark, splitRemark.size(), pageContentStream);
                remarkList.remove(0);
                currentYOffset += (20 + splitRemark.size() *10);
                continue;
            }
            int numberOfLinesLeft = (780 - currentYOffset)/10;
            printRemark(splitRemark, numberOfLinesLeft, pageContentStream);
            remarkList.remove(0);
            List<String> remainingRemarkList = splitRemark.subList(numberOfLinesLeft, splitRemark.size());
            ArrayList<String> remainingRemarkArrayList = new ArrayList<>();
            remainingRemarkArrayList.addAll(remainingRemarkList);
            currentYOffset = 0;
            PDPage currentAdditionalPage = new PDPage();
            int remainingLines = 0;
            while (remainingRemarkArrayList.size() > 0) {
                remainingLines = remainingRemarkArrayList.size();
                currentAdditionalPage = clearCurrentRemainingRemark(document, remainingRemarkArrayList);
            }
            pageContentStream = new PDPageContentStream(document, currentAdditionalPage);
            currentYOffset += (10*remainingLines);
            pageContentStream.newLineAtOffset(100, (800 - currentYOffset));
            /*int numberOfLinesLeft = (780 - currentYOffset)/10;
            if (numberOfLinesLeft > splitRemark.size()) {
                System.out.println("if");
                printRemark(splitRemark, splitRemark.size(), pageContentStream);
                remarkList.remove(0);
                currentYOffset += (20 + splitRemark.size() *10);
                System.out.println(currentYOffset);
            } else {
                System.out.println("else");
                printRemark(splitRemark, numberOfLinesLeft, pageContentStream);
                currentYOffset = 0;
                remarkList.remove(0);
                List<String> remainingRemarkList = splitRemark.subList(numberOfLinesLeft, splitRemark.size());
                ArrayList<String> remainingRemarkArrayList = new ArrayList<>();
                remainingRemarkArrayList.addAll(remainingRemarkList);
                while (remainingRemarkArrayList.size() > 0) {
                    System.out.println(remainingRemarkArrayList.size());
                    System.out.println("IM HERE");
                    clearCurrentRemainingRemark(document, remainingRemarkArrayList);
                }
            }*/
        }
        pageContentStream.endText();
        pageContentStream.close();
        return page;
    }

    /**
     * Generate new page.
     */
    private static PDPage clearCurrentRemainingRemark(PDDocument document, ArrayList<String> remarkList) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        PDPageContentStream pageContentStream = new PDPageContentStream(document, page);
        pageContentStream.setFont( PDType1Font.HELVETICA , 12 );
        pageContentStream.beginText();
        pageContentStream.newLineAtOffset(50, 800);
        int currentYOffset = 0;
        int numberOfLinesLeft = (780 - currentYOffset)/10;
        if (numberOfLinesLeft > remarkList.size()) {
            printRemark(remarkList, remarkList.size(), pageContentStream);
            remarkList.clear();
        } else {
            printRemark(remarkList, numberOfLinesLeft, pageContentStream);
            List<String> remainingRemarkList = remarkList.subList(numberOfLinesLeft, remarkList.size());
            remarkList.clear();
            remarkList.addAll(remainingRemarkList);
        }
        pageContentStream.endText();
        pageContentStream.close();
        return page;
    }

    /**
     * Prints the remark in a wrap text manner and starts at an indentation level.
     *
     * @param time the time to be printed.
     */
    private static void printTime(String time, PDPageContentStream pageContentStream) throws IOException {
        pageContentStream.newLineAtOffset(-50, 0);
        pageContentStream.showText(time);
    }

    /**
     * Prints the remark in a wrap text manner and starts at an indentation level.
     *
     * @param remarks the list of remarks to be printed.
     */
    private static void printRemark(ArrayList<String> remarks, int size, PDPageContentStream pageContentStream) throws IOException {
        pageContentStream.newLineAtOffset(50, 0);
        for (int i = 0; i < size; i++) {
            pageContentStream.showText(remarks.get(i));
            pageContentStream.newLineAtOffset(0, -10);
        }
    }

    /**
     * Splits the remark to a maximum of 80 characters per line.
     *
     * @param remark the remark to be splitted.
     */
    private static ArrayList<String> splitRemark(String remark) {
        int numberOfLines = (remark.length()/80) + 1;
        ArrayList<String> splitRemarks = new ArrayList<>();
        String[] words = remark.split(" ");

        int i = 0;
        while (splitRemarks.size() < numberOfLines) {
            StringBuilder currentBuilder = new StringBuilder();
            while (i < words.length) {
                String currentWord = words[i];
                if (currentWord.length() > 80) {
                    while (currentWord.length() > 80) {
                        int remainingCapacity = Math.min(79, 79 - currentBuilder.length());
                        currentBuilder.append(currentWord.substring(0, remainingCapacity).toString() + "-");
                        splitRemarks.add(currentBuilder.toString());
                        currentWord = currentWord.substring(remainingCapacity);
                        currentBuilder = new StringBuilder();
                    }
                    currentBuilder = new StringBuilder();
                    currentBuilder.append(currentWord + " ");
                    currentWord = words[++i];
                }
                if (currentBuilder.length() + currentWord.length() > 80) {
                    break;
                }
                currentBuilder.append(currentWord + " ");
                i++;
            }
            splitRemarks.add(currentBuilder.toString());
        }
        return splitRemarks;
    }
}
