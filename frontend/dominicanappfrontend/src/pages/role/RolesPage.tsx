import React, { useEffect, useState } from 'react';
import useHttp from '../../services/UseHttp';
import { Role } from '../../models/interfaces';
import { backendUrl } from '../../utils/constants';
import {useNavigate} from "react-router-dom";
import './RolesPage.css';

function ViewRoles() {
    const [roles, setRoles] = useState<Role[]>([]);
    const { request, error, loading } = useHttp(`${backendUrl}/api/roles`, 'GET');
    const {error: deleteRoleError, loading: deleteRoleLoading, request: deleteRoleRequest} = useHttp();
    const navigate = useNavigate();

    useEffect(() => {
        request(null, (data: Role[]) => setRoles(data));
    }, [request]);

    const handleDelete = (id: number) => {
        deleteRoleRequest(null, () => {
                request(null, (data: Role[]) => setRoles(data));
        }, false, `${backendUrl}/api/roles/${id}`, 'DELETE');
    }

    if (loading) return <div>Ładowanie...</div>;
    if (error) return <div className="error-message">{error}</div>;

    return (
        <div className="fade-in">
            <div className="d-flex justify-content-center">
                <div>
                    <h1 className="role-header">Role</h1>
                </div>
            </div>
            {deleteRoleError && <div className="error-message">{deleteRoleError}</div>}
            <table className="table table-hover table-striped table-responsive table-rounded table-shadow">
                <thead className="table-dark">
                <tr>
                    <th>ID</th>
                    <th>Nazwa</th>
                    <th>Typ</th>
                    <th>Edytuj</th>
                    <th>Usuń</th>
                </tr>
                </thead>
                <tbody>
                {roles.map(role => (
                    <tr key={role.id}>
                        <td>{role.id}</td>
                        <td>{role.name}</td>
                        <td>{role.type}</td>
                        <td>
                            <button className="btn btn-sm btn-warning"
                                    onClick={() => navigate(`/edit-role/${role.id}`)}>Edytuj
                            </button>
                        </td>
                        <td>
                            <button className="btn btn-sm btn-danger" onClick={() => handleDelete(role.id)}
                                    disabled={deleteRoleLoading}>Usuń
                            </button>
                        </td>
                    </tr>
                ))}
                </tbody>
            </table>
            <div className="d-flex justify-content-center">
                <button className="btn btn-success m-1" onClick={() => navigate('/add-role')}>Dodaj rolę</button>
            </div>
        </div>
    );
}

export default ViewRoles;
