<%@ page import="com.manydesigns.elements.xml.XhtmlBuffer" %>
<%@ page import="com.manydesigns.portofino.pageactions.calendar.EventWeek" %>
<%@ page import="com.manydesigns.portofino.pageactions.calendar.MonthView" %>
<%@ page import="org.joda.time.DateMidnight" %>
<%@ page import="org.joda.time.format.DateTimeFormatter" %>
<%@ page import="org.joda.time.format.DateTimeFormatterBuilder" %>
<%@ page import="java.util.List" %>
<%@ page contentType="text/html;charset=ISO-8859-1" language="java"
         pageEncoding="ISO-8859-1"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes-dynattr.tld"%>
<jsp:useBean id="actionBean" scope="request" type="com.manydesigns.portofino.pageactions.calendar.CalendarAction"/>
<stripes:layout-render name="/skins/${skin}/portlet.jsp">
    <stripes:layout-component name="portletTitle">
        <c:out value="${actionBean.page.title}"/>
    </stripes:layout-component>
    <stripes:layout-component name="portletBody">
        <%
            int maxEventsPerCell = 1;
            MonthView monthView = actionBean.getMonthView();
            DateTimeFormatter dayOfWeekFormatter =
                    new DateTimeFormatterBuilder()
                            .appendDayOfWeekShortText()
                            .toFormatter()
                            .withLocale(request.getLocale());
            XhtmlBuffer xhtmlBuffer = new XhtmlBuffer(out);
        %>
        <style type="text/css">
            .calendar-container {
                position: relative; height: <%= maxEventsPerCell * 120 + 120 %>px;
            }
            .days-table td, .days-table th {
                margin: 0; padding: 0 0 0 10px; border: none; text-align: left;
            }
            .calendar-table {
                position: absolute; width: 100%; margin-top: 10px; height: 100%;
                border-right: 1px solid #DDDDDD;
                border-bottom: 1px solid #DDDDDD;
            }
            .calendar-table td, .calendar-table th {
                border-style: none;
                border-top: 1px solid #DDDDDD;
                border-left: 1px solid #DDDDDD;
            }
            .calendar-container table {
                width: 100%; padding: 0; margin: 0; table-layout: fixed;
            }
            .calendar-row {
                position: absolute; width: 100%; height: <%= 100.0 / 6.0 %>%;
            }
            .grid-table {
                width: 100%; height: 100%; 
                position: absolute; left: 0px; top: 0px;
            }
            .events-table {
                position: relative; border: none;
            }
            .events-table td {
                padding: 1px; border: none;
            }
            .events-table th {
                padding: 0 0 3px 10px; border: none; text-align: left;
            }
            .event {
                padding: 0 0 0 8px;
            }
            .outOfMonth {
                color: #BBBBBB;
            }
        </style>
        <div class="calendar-container">
            <table class="days-table">
                <tr>
                    <%
                        for(int i = 0; i < 7; i++) {
                            DateMidnight dayOfWeek = monthView.getWeek(0).getDay(i).getDayStart();
                            xhtmlBuffer.openElement("th");
                            xhtmlBuffer.write(dayOfWeekFormatter.print(dayOfWeek));
                            xhtmlBuffer.closeElement("th");
                        }
                    %>
                </tr>
            </table>
            <div class="calendar-table">
                <% for(int index = 0; index < 6; index++) {
                    MonthView.Week week = monthView.getWeek(index);
                %>
                    <div class="calendar-row" style="top: <%= index * 100.0 / 6.0 %>%;">
                        <table class="grid-table">
                            <tr>
                                <td></td>
                                <td></td>
                                <td></td>
                                <td></td>
                                <td></td>
                                <td></td>
                                <td></td>
                            </tr>
                        </table>
                        <table class="events-table">
                            <tr>
                                <%
                                for(int i = 0; i < 7; i++) {
                                    MonthView.Day day = week.getDay(i);
                                    xhtmlBuffer.openElement("th");
                                    if(!day.isInReferenceMonth()) {
                                        xhtmlBuffer.addAttribute("class", "outOfMonth");
                                    }
                                    xhtmlBuffer.write(day.getDayStart().dayOfMonth().get() + "");
                                    xhtmlBuffer.closeElement("th");
                                } %>
                            </tr>
                            <tr>
                                <%
                                for(int row = 0; row < maxEventsPerCell - 1; row++) {
                                    for(int dayOfWeek = 0; dayOfWeek < 7; dayOfWeek++) {
                                        MonthView.Day day = week.getDay(dayOfWeek);
                                        List<EventWeek> eventsOfTheDay = day.getSlots();

                                        if(row < eventsOfTheDay.size()) {
                                            printEvent(day, dayOfWeek, row, xhtmlBuffer);
                                        } else {
                                            out.print("<td></td>");
                                        }
                                    }
                                }
                                //Last row
                                boolean moreThanOneLeft = false;
                                for(int dayOfWeek = 0; dayOfWeek < 7; dayOfWeek++) {
                                    MonthView.Day day = week.getDay(dayOfWeek);
                                    List<EventWeek> eventsOfTheDay = day.getSlots();

                                    if(eventsOfTheDay.size() > maxEventsPerCell) {
                                        moreThanOneLeft = true;
                                        break;
                                    }
                                }
                                for(int dayOfWeek = 0; dayOfWeek < 7; dayOfWeek++) {
                                    MonthView.Day day = week.getDay(dayOfWeek);
                                    List<EventWeek> eventsOfTheDay = day.getSlots();

                                    if(moreThanOneLeft) {
                                        xhtmlBuffer.openElement("td");
                                        if(!day.isInReferenceMonth()) {
                                            xhtmlBuffer.addAttribute("class", "outOfMonth");
                                        }
                                        xhtmlBuffer.openElement("div");
                                        xhtmlBuffer.addAttribute("class", "event");
                                        xhtmlBuffer.write(eventsOfTheDay.size() + 1 - maxEventsPerCell + " more events");
                                        xhtmlBuffer.closeElement("div");
                                        xhtmlBuffer.closeElement("td");
                                    } else {
                                        if(maxEventsPerCell - 1 < eventsOfTheDay.size()) {
                                            printEvent(day, dayOfWeek, maxEventsPerCell - 1, xhtmlBuffer);
                                        } else {
                                            out.print("<td></td>");
                                        }
                                    }
                                }
                                %>
                            </tr>
                        </table>
                    </div>
                <% } %>
            </div>
        </div>
    </stripes:layout-component>
    <stripes:layout-component name="portletFooter">
    </stripes:layout-component>
</stripes:layout-render><%!
    private void printEvent(MonthView.Day day, int dayOfWeek, int index, XhtmlBuffer xhtmlBuffer) {
        List<EventWeek> eventsOfTheDay = day.getSlots();
        EventWeek event = eventsOfTheDay.get(index);

        if(event == null) {
            xhtmlBuffer.write("<td></td>");
            return;
        }
        if(event.getStartDay() < dayOfWeek) {
            return;
        }
        xhtmlBuffer.openElement("td");
        xhtmlBuffer.addAttribute("colspan", (event.getEndDay() + 1 - dayOfWeek) + "");
        if(!day.isInReferenceMonth()) {
            xhtmlBuffer.addAttribute("class", "outOfMonth");
        }
        xhtmlBuffer.openElement("div");
        xhtmlBuffer.addAttribute("class", "event");
        xhtmlBuffer.addAttribute("style", "background-color: " + event.getEvent().getCalendar().getColor());
        xhtmlBuffer.write(event.getEvent().getDescription());
        xhtmlBuffer.closeElement("div");
        xhtmlBuffer.closeElement("td");
    }

%>