import React, { useEffect, useState } from 'react';
import useHttp from '../../services/UseHttp';
import { Schedule } from '../../models/interfaces';
import {backendUrl} from "../../utils/constants";

function SchedulePage() {
    const [scheduleList, setScheduleList] = useState<Schedule[]>([]);
    const { request: fetchSchedule, error, loading} = useHttp(`${backendUrl}/api/schedules`, 'GET');

    useEffect(() => {
        fetchSchedule(null, (data) => setScheduleList(data));
    }, [fetchSchedule]);

    if (loading) return <div>Ładowanie...</div>;
    if (error) return <div className="error-message">{error}</div>;

    return (
        <div>
            <h1>Harmonogram</h1>
            <table>
                <thead>
                <tr>
                    <th>ID</th>
                    <th>Task</th>
                    <th>User</th>
                    <th>Date</th>
                </tr>
                </thead>
                <tbody>
                {scheduleList.map(schedule => (
                    <tr key={schedule.id}>
                        <td>{schedule.id}</td>
                        <td>{schedule.task.name}</td>
                        <td>{schedule.user.name} {schedule.user.surname}</td>
                        <td>{schedule.date}</td>
                    </tr>
                ))}
                </tbody>
            </table>
        </div>
    );
}

export default SchedulePage;
