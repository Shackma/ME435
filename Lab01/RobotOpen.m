function s =RobotOpen
clc
open_ports = instrfind('Type','Serial','Status','open');
if ~isempty(open_ports)
    fclose(open_ports);
end
s = serial('COM24','BaudRate',19200,'Terminator',10,'Timeout',5);

fopen(s);
fprintf(s,'INITIALIZE');
fprintf(getResponse(s));