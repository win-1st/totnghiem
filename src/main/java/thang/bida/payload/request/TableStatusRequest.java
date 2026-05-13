package thang.bida.payload.request;

import thang.bida.model.BidaTable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TableStatusRequest {

    private BidaTable.TableStatus status;

    public TableStatusRequest() {
    }

    public TableStatusRequest(BidaTable.TableStatus status) {
        this.status = status;
    }
}