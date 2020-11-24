package at.enactmentengine.serverless.object;

/**
 * Class which represents a ListPair with start and end.
 *
 * @param <X> The end value.
 * @param <Y> The start value.
 * @author markusmoosbrugger, jakobnoeckl
 */
public class ListPair<X, Y> {
    private X start;
    private Y end;

    public ListPair(X start, Y end) {
        super();
        this.start = start;
        this.end = end;
    }

    public ListPair() {
    }

    public X getStart() {
        return start;
    }

    public void setStart(X start) {
        this.start = start;
    }

    public Y getEnd() {
        return end;
    }

    public void setEnd(Y end) {
        this.end = end;
    }

}
