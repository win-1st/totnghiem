package thang.bida.payload.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TableRequest {

    private String name;
    private Integer number;
    private Integer capacity;
}