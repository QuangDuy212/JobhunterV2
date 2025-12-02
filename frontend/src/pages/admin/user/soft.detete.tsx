import DataTable from "@/components/client/data-table";
import { useAppDispatch, useAppSelector } from "@/redux/hooks";
import { fetchUser } from "@/redux/slice/userSlide";
import { IUser } from "@/types/backend";
import { DeleteOutlined, EditOutlined, PlusOutlined, EyeOutlined, ClockCircleOutlined } from "@ant-design/icons";
import { ActionType, ProColumns } from '@ant-design/pro-components';
import { Button, Popconfirm, Space, message, notification } from "antd";
import { useState, useRef, useEffect } from 'react';
import dayjs from 'dayjs';
import { callDeleteUser, callFetchUserDeleted, callRestoreUser } from "@/config/api";
import queryString from 'query-string';
import ModalUser from "@/components/admin/user/modal.user";
import ViewDetailUser from "@/components/admin/user/view.user";
import Access from "@/components/share/access";
import { ALL_PERMISSIONS } from "@/config/permissions";
import { sfLike } from "spring-filter-query-builder";
import { fetchUserDeleted } from "@/redux/slice/userDeletedSlide";

const UserDeleted = () => {
    const [openModal, setOpenModal] = useState<boolean>(false);
    const [dataInit, setDataInit] = useState<IUser | null>(null);
    const [openViewDetail, setOpenViewDetail] = useState<boolean>(false);

    const tableRef = useRef<ActionType>();

    const isFetching = useAppSelector(state => state.userDeleted.isFetching);
    const meta = useAppSelector(state => state.userDeleted.meta);
    const users = useAppSelector(state => state.userDeleted.result);
    const dispatch = useAppDispatch();

    const handleRetoreUser = async (id: string | undefined) => {
        if (id) {
            const res = await callRestoreUser(id);
            console.log("res", res);
            if (+res.statusCode === 200) {
                message.success('Restore user successfully');
                reloadTable();
                dispatch(fetchUser({ query: "?page=1&size=10&sort=updatedAt,desc" })); // Refresh deleted users tab
            } else {
                notification.error({
                    message: 'Error occur',
                    description: res.message
                });
            }
        }
    }

    const reloadTable = () => {
        tableRef?.current?.reload();
    }

    const columns: ProColumns<IUser>[] = [
        {
            title: 'No',
            key: 'index',
            width: 50,
            align: "center",
            render: (text, record, index) => {
                return (
                    <>
                        {(index + 1) + (meta.page - 1) * (meta.pageSize)}
                    </>)
            },
            hideInSearch: true,
        },
        {
            title: 'Name',
            dataIndex: 'name',
            sorter: true,
        },
        {
            title: 'Email',
            dataIndex: 'email',
            sorter: true,
        },

        {
            title: 'Role',
            dataIndex: ["role", "name"],
            sorter: true,
            hideInSearch: true
        },

        {
            title: 'Company',
            dataIndex: ["company", "name"],
            sorter: true,
            hideInSearch: true
        },

        {
            title: 'CreatedAt',
            dataIndex: 'createdAt',
            width: 200,
            sorter: true,
            render: (text, record, index, action) => {
                return (
                    <>{record.createdAt ? dayjs(record.createdAt).format('DD-MM-YYYY HH:mm:ss') : ""}</>
                )
            },
            hideInSearch: true,
        },
        {
            title: 'UpdatedAt',
            dataIndex: 'updatedAt',
            width: 200,
            sorter: true,
            render: (text, record, index, action) => {
                return (
                    <>{record.updatedAt ? dayjs(record.updatedAt).format('DD-MM-YYYY HH:mm:ss') : ""}</>
                )
            },
            hideInSearch: true,
        },
        {

            title: 'Actions',
            hideInSearch: true,
            width: 50,
            render: (_value, entity, _index, _action) => (
                <Space>
                    < Access
                        permission={ALL_PERMISSIONS.USERS.RESTORE}
                        hideChildren
                    >
                        <ClockCircleOutlined
                            style={{
                                fontSize: 20,
                                color: '#ffa500',
                            }}
                            type=""
                            onClick={() => { handleRetoreUser(entity.id) }} />
                    </Access >
                </Space >
            ),

        },
    ];

    const buildQuery = (params: any, sort: any, filter: any) => {
        const q: any = {
            page: params.current,
            size: params.pageSize,
            filter: ""
        }

        const clone = { ...params };
        if (clone.name) q.filter = `${sfLike("name", clone.name)}`;
        if (clone.email) {
            q.filter = clone.name ?
                q.filter + " and " + `${sfLike("email", clone.email)}`
                : `${sfLike("email", clone.email)}`;
        }

        if (!q.filter) delete q.filter;
        let temp = queryString.stringify(q);

        let sortBy = "";
        if (sort && sort.name) {
            sortBy = sort.name === 'ascend' ? "sort=name,asc" : "sort=name,desc";
        }
        if (sort && sort.email) {
            sortBy = sort.email === 'ascend' ? "sort=email,asc" : "sort=email,desc";
        }
        if (sort && sort.createdAt) {
            sortBy = sort.createdAt === 'ascend' ? "sort=createdAt,asc" : "sort=createdAt,desc";
        }
        if (sort && sort.updatedAt) {
            sortBy = sort.updatedAt === 'ascend' ? "sort=updatedAt,asc" : "sort=updatedAt,desc";
        }

        //mặc định sort theo updatedAt
        if (Object.keys(sortBy).length === 0) {
            temp = `${temp}&sort=updatedAt,desc`;
        } else {
            temp = `${temp}&${sortBy}`;
        }

        return temp;
    }

    return (
        <div>
            <Access
                permission={ALL_PERMISSIONS.USERS.GET_PAGINATE}
            >
                <DataTable<IUser>
                    actionRef={tableRef}
                    headerTitle="List Users"
                    rowKey="id"
                    loading={isFetching}
                    columns={columns}
                    dataSource={users}
                    request={async (params, sort, filter): Promise<any> => {
                        const query = buildQuery(params, sort, filter);
                        dispatch(fetchUserDeleted({ query }))
                    }}
                    scroll={{ x: true }}
                    pagination={
                        {
                            current: meta.page,
                            pageSize: meta.pageSize,
                            showSizeChanger: true,
                            total: meta.total,
                            showTotal: (total, range) => { return (<div> {range[0]}-{range[1]} per {total} rows</div>) }
                        }
                    }
                    rowSelection={false}
                />
            </Access>
            <ModalUser
                openModal={openModal}
                setOpenModal={setOpenModal}
                reloadTable={reloadTable}
                dataInit={dataInit}
                setDataInit={setDataInit}
            />
            <ViewDetailUser
                onClose={setOpenViewDetail}
                open={openViewDetail}
                dataInit={dataInit}
                setDataInit={setDataInit}
            />
        </div >
    )
}

export default UserDeleted;